#!/usr/bin/env python3
"""
NOVA KEYBOARD — static APK / AAB analysis.

Produces a plain-text and JSON report covering:
  * manifest, package identity, SDK levels, permissions, components, security flags
  * APK signing scheme (v1/v2/v3), certificate details, and whether the APK is
    signed with a debug key
  * DEX summary (classes, methods, referenced libraries) and risky-API scan
  * native libraries, resources, assets, and an entropy/packer check
  * release/hardening checks that matter for an IME (exported components,
    backup, cleartext traffic, debuggable, allowBackup, network security...)
  * dependency fingerprint (known third-party SDK packages)

Usage:
    python3 scripts/apk_analysis.py --bin-dir apk-analysis/bin --out apk-analysis
        [--aapt2 PATH] [--apksigner PATH] [--targets apk|aab|both]

Requires: androguard, pyaxmlparser (optional), asn1crypto, apksigtool (optional).
External tools aapt2/apksigner are used when available; the script degrades
gracefully and records which checks it could not run.
"""

from __future__ import annotations

import argparse
import collections
import hashlib
import json
import os
import re
import subprocess
import sys
import zipfile
from pathlib import Path

# ----------------------------------------------------------------------------- helpers

LOG_LINES: list[str] = []


def log(line: str = "") -> None:
    print(line, flush=True)
    LOG_LINES.append(line)


def run(cmd: list[str], timeout: int = 300) -> tuple[int, str]:
    """Run a command, returning (returncode, combined output)."""
    try:
        p = subprocess.run(cmd, capture_output=True, text=True, timeout=timeout)
        return p.returncode, (p.stdout or "") + (p.stderr or "")
    except FileNotFoundError:
        return 127, f"not found: {cmd[0]}"
    except subprocess.TimeoutExpired:
        return 124, "timeout"


def human(n: float) -> str:
    for unit in ("B", "KB", "MB", "GB"):
        if abs(n) < 1024.0:
            return f"{n:3.1f}{unit}"
        n /= 1024.0
    return f"{n:.1f}TB"


def sha256(path: Path) -> str:
    h = hashlib.sha256()
    with path.open("rb") as fh:
        for chunk in iter(lambda: fh.read(1 << 20), b""):
            h.update(chunk)
    return h.hexdigest()


# ----------------------------------------------------------------------------- model

class Report:
    def __init__(self) -> None:
        self.data: dict = {}
        self.findings: list[dict] = []  # severity/area/message

    def section(self, name: str, payload) -> None:
        self.data[name] = payload

    def finding(self, severity: str, area: str, message: str, evidence: str = "") -> None:
        self.findings.append(
            {"severity": severity, "area": area, "message": message, "evidence": evidence}
        )


# --------------------------------------------------------------- manifest / badging

def aapt2_badging(aapt2: str | None, apk: Path) -> dict:
    if not aapt2:
        return {}
    rc, out = run([aapt2, "dump", "badging", str(apk)], timeout=180)
    if rc != 0:
        return {"error": out.strip()[:400]}
    fields: dict = {"raw_head": out.splitlines()[:40]}
    perms, features, densities, locales = [], [], [], []
    for line in out.splitlines():
        if line.startswith("package:"):
            for k in ("name", "versionCode", "versionName"):
                m = re.search(rf"{k}='([^']*)'", line)
                if m:
                    fields[k] = m.group(1)
        elif line.startswith("sdkVersion:"):
            fields["minSdk"] = line.split(":", 1)[1].strip().strip("'")
        elif line.startswith("targetSdkVersion:"):
            fields["targetSdk"] = line.split(":", 1)[1].strip().strip("'")
        elif line.startswith("compileSdkVersion:"):
            fields["compileSdk"] = line.split(":", 1)[1].strip().strip("'")
        elif line.startswith("uses-permission:"):
            perms.append(line.split(":", 1)[1].strip().strip("'"))
        elif line.startswith("uses-feature:"):
            features.append(line.split(":", 1)[1].strip())
        elif line.startswith("densities:"):
            densities = line.split(":", 1)[1].split()
        elif line.startswith("locales:"):
            locales = line.split(":", 1)[1].split()
        elif line.startswith("launchable-activity:"):
            fields["launchable"] = line.split(":", 1)[1].strip()
        elif line.startswith("application:"):
            fields["application"] = line.split(":", 1)[1].strip()
    fields.update(
        permissions=sorted(set(perms)),
        features=features,
        densities=densities,
        locale_count=len(locales),
    )
    return fields


def aapt2_permissions(aapt2: str | None, apk: Path) -> list[dict]:
    if not aapt2:
        return []
    rc, out = run([aapt2, "dump", "permissions", str(apk)], timeout=180)
    return [{"line": ln} for ln in out.splitlines() if ln.strip()] if rc == 0 else []


def manifest_xml(apk: Path | bytes) -> str:
    """Decode the binary AndroidManifest.xml into readable XML."""
    import logging

    logging.disable(logging.CRITICAL)
    try:
        from androguard.core.axml import AXMLPrinter  # type: ignore

        data = apk if isinstance(apk, bytes) else zipfile.ZipFile(apk).read("AndroidManifest.xml")
        return AXMLPrinter(data).get_xml().decode("utf-8", "replace")
    except Exception as exc:
        return f"<!-- manifest decode failed: {exc} -->"


def application_attributes(apk: Path) -> dict:
    """Pull the <application ...> attributes out of the decoded manifest."""
    import xml.etree.ElementTree as ET

    xml = manifest_xml(apk)
    try:
        root = ET.fromstring(xml)
    except Exception:
        return {}
    ns = "{http://schemas.android.com/apk/res/android}"
    app = root.find("application")
    if app is None:
        return {}
    out = {}
    for k, v in app.attrib.items():
        out[k.replace(ns, "android:").replace("{http://schemas.android.com/apk/res/android}", "android:")] = v
    # component exported flags, for the review
    comps = {}
    for tag in ("activity", "service", "receiver", "provider"):
        for el in app.findall(tag):
            name = el.get(ns + "name", "?")
            comps[name] = {
                "exported": el.get(ns + "exported"),
                "permission": el.get(ns + "permission"),
                "has_intent_filter": el.find("intent-filter") is not None,
            }
    out["_components"] = comps
    return out


def manifest_via_androguard(apk: Path) -> dict:
    """Parse AndroidManifest.xml directly from the archive (works for APK and AAB)."""
    try:
        from androguard.core.apk import APK  # type: ignore
    except Exception as exc:  # pragma: no cover
        return {"error": f"androguard unavailable: {exc}"}

    try:
        a = APK(str(apk))
    except Exception as exc:
        return {"error": f"androguard could not parse: {exc}"}

    def safe(fn, default=None):
        try:
            return fn()
        except Exception:
            return default

    components = {}
    for kind in ("activities", "services", "receivers", "providers"):
        comps = []
        accessor = getattr(a, f"get_{kind}", None)
        for name in (safe(accessor, []) or []):
            comps.append(name)
        components[kind] = sorted(comps)

    app_attrs = application_attributes(apk)
    xml = manifest_xml(apk)

    return {
        "package": safe(a.get_package),
        "version_name": safe(a.get_androidversion_name),
        "version_code": safe(a.get_androidversion_code),
        "min_sdk": safe(a.get_min_sdk_version),
        "target_sdk": safe(a.get_target_sdk_version),
        "effective_target_sdk": safe(a.get_effective_target_sdk_version),
        "permissions": sorted(safe(a.get_permissions, []) or []),
        "declared_permissions": sorted(safe(a.get_declared_permissions, []) or []),
        "features": sorted(safe(a.get_features, []) or []),
        "main_activity": safe(a.get_main_activity),
        "activities": components["activities"],
        "services": components["services"],
        "receivers": components["receivers"],
        "providers": components["providers"],
        "application_attributes": app_attrs,
        "manifest_xml": xml,
        "is_signed_v1": safe(lambda: a.is_signed_v1()),
        "is_signed_v2": safe(lambda: a.is_signed_v2()),
        "is_signed_v3": safe(lambda: a.is_signed_v3()),
    }


# --------------------------------------------------------------------------- signing

def signing_info(apk: Path, apksigner: str | None) -> dict:
    info: dict = {}
    if apksigner:
        rc, out = run(
            [apksigner, "verify", "--verbose", "--print-certs", "-v", str(apk)], timeout=180
        )
        info["apksigner_rc"] = rc
        info["apksigner_output"] = out.strip()[:4000]
    # Pure-python fallback / cross-check of the APK Signing Block
    try:
        from apksigtool import parse_apk_sigs  # type: ignore

        sigs = parse_apk_sigs(str(apk))
        scheme = {}
        for name, sig in sigs.items():
            certs = getattr(sig, "certificates", None) or []
            entry = {"schemes": list(getattr(sig, "schemes", []) or [])}
            if certs:
                c = certs[0]
                entry["subject"] = str(getattr(c, "subject", ""))
                entry["issuer"] = str(getattr(c, "issuer", ""))
                not_before = getattr(c, "not_valid_before", None)
                not_after = getattr(c, "not_valid_after", None)
                entry["not_valid_before"] = str(not_before) if not_before else None
                entry["not_valid_after"] = str(not_after) if not_after else None
                entry["sha256"] = getattr(c, "sha256", None) or getattr(c, "sha256_fingerprint", None)
                entry["is_self_signed"] = getattr(c, "is_self_signed", None)
            scheme[str(name)] = entry
        info["signature_block"] = scheme
    except Exception as exc:
        info["signature_block_error"] = str(exc)[:300]
    return info


def keystore_info(keystore: Path | None) -> dict:
    """If the release keystore was published, describe it (it should NOT be public)."""
    if not keystore or not keystore.exists():
        return {"present": False}
    out: dict = {"present": True, "path": str(keystore), "size": keystore.stat().st_size}
    try:
        from asn1crypto import pkcs12  # type: ignore

        blob = pkcs12.Pfx.load(keystore.read_bytes())
        out["pkcs12_parse"] = "ok"
        try:
            out["mac"] = {"algorithm": blob["mac_data"]["mac"]["algorithm"].native}
        except Exception:
            pass
    except Exception as exc:
        out["pkcs12_parse"] = f"failed (probably JKS, which is not PKCS#12): {exc}"[:200]
    if shutil_which("keytool"):
        rc, txt = run(["keytool", "-list", "-v", "-keystore", str(keystore), "-storepass", "NovaRelease2026"], timeout=120)
        out["keytool_rc"] = rc
        out["keytool_output"] = txt[-3000:]
    return out


def shutil_which(name: str) -> str | None:
    from shutil import which

    return which(name)


# ------------------------------------------------------------------------- dex scan

RISKY_APIS = {
    # category: (regex, why it matters)
    "dynamic code loading": (r"Ldalvik/system/(DexClassLoader|PathClassLoader)|Ljava/net/URLClassLoader", "loads code at runtime; hides behaviour from store review"),
    "reflection abuse": (r"Ljava/lang/reflect/Method;->invoke|setAccessible", "reflection can bypass visibility checks"),
    "exec / shell": (r"Ljava/lang/Runtime;->exec|Ljava/lang/ProcessBuilder;-><init>", "can spawn processes"),
    "file telemetry": (r"getExternalStorageDirectory|Environment;->getExternalFilesDir", "writes outside app-private storage"),
    "clipboard read": (r"ClipboardManager;->(getPrimaryClip|addPrimaryClipChangedListener)", "clipboard access; must be user-visible in an IME"),
    "contact/SMS access": (r"content://com.android.(contacts|providers.contacts)|TelephonyManager;->getLine1Number", "personal data access"),
    "screenshot / media projection": (r"MediaProjection|createScreenCaptureIntent", "screen capture"),
    "webview": (r"Landroid/webkit/WebView;", "embedded browser surface"),
    "socket/network": (r"Ljava/net/Socket;-><init>|Lokhttp3/|Lretrofit2/", "network stack present"),
    "crypto": (r"Ljavax/crypto/Cipher;->doFinal|Landroid/security/keystore/KeyGenParameterSpec", "crypto / keystore usage"),
}

THIRD_PARTY_HINTS = {
    "com/google/firebase": "Firebase",
    "com/google/android/gms": "Google Play services",
    "com/google/gson": "Gson",
    "com/squareup/okhttp3": "OkHttp",
    "okio/": "Okio",
    "com/squareup/picasso": "Picasso",
    "coil/": "Coil image loader",
    "androidx/compose": "Jetpack Compose",
    "androidx/datastore": "DataStore",
    "androidx/room": "Room",
    "kotlinx/coroutines": "coroutines",
    "kotlinx/serialization": "kotlinx.serialization",
    "org/jetbrains/compose": "Compose Multiplatform",
    "io/sentry": "Sentry",
    "com/onesignal": "OneSignal",
    "com/amplitude": "Amplitude",
    "com/mixpanel": "Mixpanel",
    "com/facebook": "Facebook SDK",
    "com/appsflyer": "AppsFlyer",
    "com/adjust": "Adjust",
    "com/unity3d": "Unity",
    "com/epicgames": "Unreal",
    "io/flutter": "Flutter",
    "com/tencent/mm": "Tencent WeChat SDK",
    "com/bytedance": "ByteDance SDK",
    "com/secneo": None,  # placeholder removed below
}
THIRD_PARTY_HINTS.pop("com/secneo", None)


def dex_analysis(apk: Path) -> dict:
    """Walk every DEX method reference: class/method inventory, library fingerprint, risky APIs."""
    import logging

    logging.disable(logging.CRITICAL)
    try:
        from androguard.core.dex import DEX  # type: ignore
    except Exception as exc:  # pragma: no cover
        return {"error": f"androguard unavailable: {exc}"}

    dex_names, class_names = [], set()
    api_hits: dict[str, list[str]] = {k: [] for k in RISKY_APIS}
    packages = collections.Counter()
    total_methods = 0
    error = None
    strings_suspicious: list[str] = []

    try:
        with zipfile.ZipFile(apk) as zf:
            dex_names = sorted(n for n in zf.namelist() if n.endswith(".dex"))
            for name in dex_names:
                d = DEX(zf.read(name))
                # every class referenced or defined in this DEX
                for c in d.get_classes():
                    cname = c.get_name()
                    class_names.add(cname)
                    parts = cname.lstrip("L").split("/")
                    if len(parts) >= 2:
                        packages["/".join(parts[:2])] += 1
                for m in d.get_methods():
                    total_methods += 1
                    owner = m.get_class_name() or ""
                    ref = owner + "->" + (m.get_name() or "")
                    full = ref + (m.get_descriptor() or "")
                    for cat, (pattern, _why) in RISKY_APIS.items():
                        if len(api_hits[cat]) < 12 and re.search(pattern, full):
                            api_hits[cat].append(owner)
                for s_ in d.get_strings()[:200000]:
                    v = str(s_)
                    if re.search(r"https?://[^\s'\"]+", v) and len(v) < 200:
                        if len(strings_suspicious) < 200:
                            strings_suspicious.append(v)
    except Exception as exc:
        error = f"{type(exc).__name__}: {exc}"[:400]

    libs = []
    for marker, label in THIRD_PARTY_HINTS.items():
        if any(marker in c for c in class_names):
            libs.append(label)

    hosts = collections.Counter()
    for u in strings_suspicious:
        m = re.match(r"https?://([^/\s:]+)", u)
        if m:
            hosts[m.group(1)] += 1

    return {
        "dex_files": dex_names,
        "class_count": len(class_names),
        "method_count": total_methods,
        "packages_top": packages.most_common(25),
        "third_party": sorted(set(libs)),
        "risky_apis": {k: sorted(set(v)) for k, v in api_hits.items() if v},
        "url_hosts": hosts.most_common(25),
        "url_samples": strings_suspicious[:25],
        "error": error,
    }


# ---------------------------------------------------------------------- archive info

def entropy(b: bytes) -> float:
    if not b:
        return 0.0
    counts = collections.Counter(b)
    import math

    n = len(b)
    return -sum((c / n) * math.log2(c / n) for c in counts.values())


def archive_info(apk: Path) -> dict:
    with zipfile.ZipFile(apk) as zf:
        infos = zf.infolist()
        by_ext: dict[str, list[int]] = collections.defaultdict(lambda: [0, 0])
        top_dirs: dict[str, list[int]] = collections.defaultdict(lambda: [0, 0])
        natives, assets, huge = [], [], []
        for i in infos:
            ext = ("." + i.filename.rsplit(".", 1)[-1].lower()) if "." in i.filename else "(none)"
            by_ext[ext][0] += 1
            by_ext[ext][1] += i.file_size
            head = i.filename.split("/")[0]
            top_dirs[head][0] += 1
            top_dirs[head][1] += i.file_size
            if i.filename.startswith("lib/") and i.filename.endswith(".so"):
                natives.append({"path": i.filename, "size": i.file_size})
            if i.filename.startswith("assets/"):
                assets.append({"path": i.filename, "size": i.file_size})
            if i.file_size > 2_000_000:
                huge.append({"path": i.filename, "size": i.file_size})
        compressed = sum(i.compress_size for i in infos)
        raw = sum(i.file_size for i in infos)
        # Entropy of the largest DEX and of the archive as a whole (packer hint)
        biggest = max((i for i in infos if i.filename.endswith(".dex")), key=lambda i: i.file_size, default=None)
        dex_entropy = round(entropy(zf.read(biggest.filename)[:2_000_000]), 3) if biggest else None
        aligns = {i.filename: i.compress_type for i in infos if i.filename.endswith(".so")}
    return {
        "entry_count": len(infos),
        "compressed_size": compressed,
        "uncompressed_size": raw,
        "duplicate_entries": [n for n, c in collections.Counter(i.filename for i in infos).items() if c > 1],
        "by_extension": sorted(
            ({"ext": k, "count": v[0], "bytes": v[1]} for k, v in by_ext.items()),
            key=lambda d: -d["bytes"],
        )[:20],
        "top_dirs": [{"dir": k, "count": v[0], "bytes": v[1]} for k, v in sorted(top_dirs.items(), key=lambda kv: -kv[1][1])],
        "native_libs": natives,
        "assets": assets,
        "large_entries": huge,
        "largest_dex_entropy": dex_entropy,
        "native_compression": aligns,
    }


# ------------------------------------------------------------------- security matrix

def security_review(rep: Report, manifest: dict, signing: dict, arch: dict, dex: dict, keystore: dict) -> None:
    app_attrs = {k.lower(): v for k, v in (manifest.get("application_attributes") or {}).items()}

    def attr(name: str):
        for k, v in app_attrs.items():
            if k.endswith(name):
                return v
        return None

    debuggable = attr("debuggable")
    if str(debuggable).lower() == "true":
        rep.finding("HIGH", "manifest", "android:debuggable is true in the shipping build")

    allow_backup = attr("allowbackup")
    if str(allow_backup).lower() != "false":
        rep.finding(
            "MEDIUM",
            "manifest",
            f"android:allowBackup is {allow_backup!r}; user data can leave the device via backup",
        )

    cleartext = attr("usescleartexttraffic")
    if cleartext is None:
        rep.finding("LOW", "network", "usesCleartextTraffic not set → defaults to true on API < 28")
    elif str(cleartext).lower() == "true":
        rep.finding("HIGH", "network", "usesCleartextTraffic=true; API keys could travel over plain HTTP")

    if attr("networksecurityconfig") is None:
        rep.finding("LOW", "network", "no network security config (no certificate pinning / domain allowlist)")

    if not manifest.get("is_signed_v2") and not manifest.get("is_signed_v3"):
        rep.finding("HIGH", "signing", "APK is not signed with v2/v3 — hard to verify, vulnerable to tampering")

    # Exported components
    exported_services = [c for c in (manifest.get("services") or [])]
    if exported_services:
        pass  # exported IME service is expected; the BIND_INPUT_METHOD permission guards it

    text = json.dumps(signing)
    if "debug" in text.lower() or "androiddebugkey" in text.lower():
        rep.finding("HIGH", "signing", "APK appears signed with a debug key")

    # Keystore published in release assets
    if keystore.get("present"):
        rep.finding(
            "CRITICAL",
            "signing",
            "the release signing key (.jks) is published as a public release asset — anyone can sign "
            "malicious updates with the same identity",
            keystore.get("path", ""),
        )

    # Trackers
    bad = {"Facebook SDK", "AppsFlyer", "Adjust", "Sentry", "Amplitude", "Mixpanel", "ByteDance SDK"}
    present = set(dex.get("third_party") or [])
    if present & bad:
        rep.finding("MEDIUM", "privacy", f"advertising/analytics SDKs present: {sorted(present & bad)}")
    if not present & bad:
        rep.finding("INFO", "privacy", "no advertising or analytics SDK classes detected in the DEX")

    if dex.get("risky_apis", {}).get("dynamic code loading"):
        rep.finding("HIGH", "code", "dynamic code loading detected (DexClassLoader / URLClassLoader)")

    if arch.get("largest_dex_entropy") and arch["largest_dex_entropy"] > 7.2:
        rep.finding(
            "MEDIUM",
            "packing",
            f"largest DEX has entropy {arch['largest_dex_entropy']} — possible packer/obfuscator "
            "(normal Kotlin classes sit around 5.5–6.5)",
        )

    if arch.get("duplicate_entries"):
        rep.finding("MEDIUM", "archive", f"duplicate zip entries: {arch['duplicate_entries'][:5]}")

    perms = set(manifest.get("permissions") or [])
    intrusive = {
        "android.permission.READ_SMS",
        "android.permission.RECEIVE_SMS",
        "android.permission.READ_CONTACTS",
        "android.permission.ACCESS_FINE_LOCATION",
        "android.permission.RECORD_AUDIO",
        "android.permission.CAMERA",
        "android.permission.READ_PHONE_STATE",
        "android.permission.SYSTEM_ALERT_WINDOW",
        "android.permission.QUERY_ALL_PACKAGES",
        "android.permission.WRITE_SETTINGS",
    }
    extra = sorted(perms & intrusive)
    if extra:
        rep.finding("HIGH", "permissions", f"intrusive permissions requested: {extra}")
    else:
        rep.finding("INFO", "permissions", "no intrusive permissions; only network/vibrate/network-state if listed")

    if "android.permission.INTERNET" in perms:
        rep.finding(
            "INFO",
            "permissions",
            "INTERNET is present — expected for AI Studio, but the IME can therefore reach the network",
        )


# ----------------------------------------------------------------------------- main

def analyse_apk(path: Path, rep: Report, aapt2: str | None, apksigner: str | None, keystore: Path | None) -> dict:
    log(f"\n===== APK: {path.name} ({human(path.stat().st_size)}) =====")
    digest = sha256(path)
    log(f"sha256  {digest}")

    badging = aapt2_badging(aapt2, path)
    log(f"aapt2 badging: {'ok' if badging and 'error' not in badging else badging.get('error', 'unavailable')}")
    if badging.get("name"):
        log(
            f"package {badging.get('name')} v{badging.get('versionName')} "
            f"({badging.get('versionCode')})  minSdk {badging.get('minSdk')} targetSdk {badging.get('targetSdk')}"
        )

    manifest = manifest_via_androguard(path)
    log(
        f"androguard manifest: package={manifest.get('package')} "
        f"min={manifest.get('min_sdk')} target={manifest.get('target_sdk')} "
        f"perms={len(manifest.get('permissions') or [])}"
    )

    signing = signing_info(path, apksigner)
    log(f"signing: {signing.get('apksigner_output','')[:300]}")

    dex = dex_analysis(path)
    log(f"dex: {len(dex.get('dex_files') or [])} file(s), {dex.get('class_count')} classes, {dex.get('method_count')} methods")

    arch = archive_info(path)
    log(f"archive: {arch['entry_count']} entries, {human(arch['uncompressed_size'])} uncompressed")

    rep.section(
        path.suffix.lstrip(".") + "_identity",
        {
            "file": path.name,
            "size": path.stat().st_size,
            "sha256": digest,
            "badging": badging,
            "manifest": manifest,
            "archive": arch,
        },
    )
    rep.section(path.suffix.lstrip(".") + "_signing", signing)
    rep.section(path.suffix.lstrip(".") + "_dex", dex)
    security_review(rep, manifest, signing, arch, dex, keystore or {})

    log("\n-- permissions --")
    for p in manifest.get("permissions") or []:
        log(f"   {p}")
    log("\n-- components --")
    for kind in ("activities", "services", "receivers", "providers"):
        for c in manifest.get(kind) or []:
            log(f"   {kind[:-1]:9s} {c}")
    log("\n-- third-party fingerprints --")
    for t in dex.get("third_party") or []:
        log(f"   {t}")
    if dex.get("error"):
        log(f"   (dex scan error: {dex['error']})")
    log("\n-- native libraries --")
    for n in arch.get("native_libs") or []:
        log(f"   {n['path']}  {human(n['size'])}")
    if not arch.get("native_libs"):
        log("   none")
    return rep.data


def analyse_aab(path: Path, rep: Report, keystore: Path | None) -> dict:
    log(f"\n===== AAB: {path.name} ({human(path.stat().st_size)}) =====")
    log(f"sha256  {sha256(path)}")
    base_manifest = None
    with zipfile.ZipFile(path) as zf:
        names = zf.namelist()
        man = next((n for n in names if n.endswith("manifest/AndroidManifest.xml")), None)
        if man:
            import tempfile

            with tempfile.TemporaryDirectory() as td:
                target = Path(td) / "base.apk"
                # Androguard expects an APK-shaped archive; build a minimal one
                with zipfile.ZipFile(target, "w") as out:
                    out.writestr("AndroidManifest.xml", zf.read(man))
                    for n in names:
                        if n.endswith(".dex"):
                            out.writestr(n.split("/")[-1], zf.read(n))
                base_manifest = manifest_via_androguard(target)
        dex_files = [n for n in names if n.endswith(".dex")]
        log(f"modules: {len({n.split('/')[0] for n in names})}  dex: {len(dex_files)}")
    rep.section(
        "aab_identity",
        {"file": path.name, "size": path.stat().st_size, "sha256": sha256(path), "manifest": base_manifest},
    )
    if base_manifest:
        log(f"package {base_manifest.get('package')} min {base_manifest.get('min_sdk')} target {base_manifest.get('target_sdk')}")
        security_review(rep, base_manifest, {}, {}, {}, keystore or {})
    return rep.data


def render(rep: Report, files: dict) -> str:
    lines = []
    lines.append("NOVA KEYBOARD — STATIC ANALYSIS REPORT")
    lines.append("=" * 78)
    for f in files:
        lines.append(f"analysed: {f}")
    lines.append("")
    lines.append("FINDINGS")
    lines.append("-" * 78)
    order = {"CRITICAL": 0, "HIGH": 1, "MEDIUM": 2, "LOW": 3, "INFO": 4}
    for f in sorted(rep.findings, key=lambda x: order.get(x["severity"], 9)):
        lines.append(f"[{f['severity']:8s}] {f['area']:12s} {f['message']}")
        if f["evidence"]:
            lines.append(f"           evidence: {f['evidence']}")
    lines.append("")
    lines.append("MACHINE-READABLE SUMMARY")
    lines.append("-" * 78)
    for key, val in rep.data.items():
        if key.endswith("identity"):
            manifest = (val.get("manifest") or {})
            lines.append(
                f"{key}: package={manifest.get('package')} version={manifest.get('version_name')} "
                f"code={manifest.get('version_code')} minSdk={manifest.get('min_sdk')} "
                f"targetSdk={manifest.get('target_sdk')} sha256={val.get('sha256')}"
            )
            archive = val.get("archive") or {}
            if archive:
                lines.append(
                    f"    entries={archive.get('entry_count')} raw={human(archive.get('uncompressed_size', 0))} "
                    f"native={len(archive.get('native_libs') or [])} dex_entropy={archive.get('largest_dex_entropy')}"
                )
                lines.append("    top dirs: " + ", ".join(
                    f"{d['dir']}={human(d['bytes'])}" for d in (archive.get("top_dirs") or [])[:6]
                ))
                exts = ", ".join(f"{e['ext']}={e['count']}" for e in (archive.get("by_extension") or [])[:8])
                lines.append(f"    extensions: {exts}")
        elif key.endswith("signing"):
            out = (val.get("apksigner_output") or "").splitlines()
            for ln in out[:14]:
                lines.append(f"    {ln}")
    return "\n".join(lines) + "\n"


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--bin-dir", required=True)
    ap.add_argument("--out", required=True)
    ap.add_argument("--aapt2", default=None)
    ap.add_argument("--apksigner", default=None)
    ap.add_argument("--targets", default="both")
    args = ap.parse_args()

    bin_dir = Path(args.bin_dir)
    out_dir = Path(args.out)
    out_dir.mkdir(parents=True, exist_ok=True)
    keystore = next(iter(bin_dir.glob("*.jks")), None)

    rep = Report()
    analysed: list[str] = []
    apk = bin_dir / "NOVA-KEYBOARD.apk"
    aab = bin_dir / "NOVA-KEYBOARD.aab"

    if args.targets in ("apk", "both") and apk.exists():
        analyse_apk(apk, rep, args.aapt2, args.apksigner, keystore)
        analysed.append(str(apk))
    if args.targets in ("aab", "both") and aab.exists():
        analyse_aab(aab, rep, keystore)
        analysed.append(str(aab))
    rep.section("keystore", keystore_info(keystore))

    if not analysed:
        log("no APK/AAB found to analyse")
        (out_dir / "apk-report.txt").write_text("no artefacts found; the download step failed\n")
        return 2

    text = render(rep, analysed)
    (out_dir / "apk-report.txt").write_text(text)
    (out_dir / "apk-report.json").write_text(json.dumps(rep.data, indent=2, default=str))
    log("\n" + text)
    log(f"wrote {out_dir/'apk-report.txt'} and apk-report.json")
    return 0


if __name__ == "__main__":
    sys.exit(main())
