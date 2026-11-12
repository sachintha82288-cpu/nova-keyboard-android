# NOVA KEYBOARD — APK / AAB analysis report

| | |
|---|---|
| Artefacts | `NOVA-KEYBOARD.apk` (19,637,774 B) and `NOVA-KEYBOARD.aab` (6,769,826 B), release `v1.0.0` |
| SHA-256 | APK `7a5a9da9…e7c9` · AAB `9e09b4aa…04a0` — **both match the published `checksums.txt`** |
| Package | `com.nova.keyboard` · versionCode 1 · versionName 1.0.0 |
| SDK | minSdk 29 (Android 10) · targetSdk 34 · compileSdk 34 |
| Toolchain used | JDK 17 (Temurin via `jdk4py`), aapt2 2.19, apksigner 34.x, apkanalyzer-class Python stack (androguard 4.1.4, pyaxmlparser, apksigtool), apktool 2.4.1, jarsigner + keytool |
| Analysed | 2026-11-12 |

> **How this was produced.** The sandbox running this session can reach `github.com`, `pypi.org` and
> `registry.npmjs.org` only — GitHub's release CDN (`release-assets.githubusercontent.com`) and the
> Android SDK/Maven hosts are unreachable, so the APK could not be downloaded into the shell directly.
> The toolchain was therefore built locally (`scripts/setup_apk_toolchain.sh`) and the release
> binaries were fetched by `.github/workflows/apk-analysis.yml`, which runs `scripts/apk_analysis.py`
> and commits the machine output back to the branch (`apk-analysis/apk-report.{txt,json}`,
> `apk-analysis/aab-manifest.xml`). Every number below comes from those files.

---

## 1. Verdict

| Area | Result |
|---|---|
| Integrity | **Good.** Both artefacts match the published checksums; the APK is a valid, verifiable signature. |
| Signature | **Valid but narrow.** APK Signature Scheme **v2 only** — no v1, no v3/v3.1 proof-of-rotation. 1 signer, 4096-bit RSA, `CN=NOVA KEYBOARD, O=NOVA, L=Colombo, C=LK`, SHA-256 `bfffe869…43a9b`, self-signed, valid 2026-11-12 → 2056-11-04. AAB verifies with `jarsigner` under the same certificate. |
| **Release-key exposure** | **CRITICAL.** The private signing key is published as a release asset (see §3.1). |
| Bloat / trackers | **Clean.** No ad, analytics or attribution SDK classes. Third-party code = AndroidX/Compose, DataStore, OkHttp/Okio, kotlinx.coroutines/serialization. |
| Permissions | **Minimal and consistent with the privacy policy**: INTERNET, ACCESS_NETWORK_STATE, VIBRATE + one self-declared `signature`-level permission. No SMS, contacts, location, camera, mic, storage, accessibility or overlay permissions. |
| Hardening | `allowBackup=false`, `usesCleartextTraffic=false`, `extractNativeLibs=false`, data-extraction rules present, IME service guarded by `BIND_INPUT_METHOD`, launcher activity exported (as required). |
| **Source ↔ binary provenance** | **Mismatch.** The shipped APK was built from the `v1.0.0` tag, **not** from `main`/`3ae336e` ("fix: harden every startup path"). See §3.4 — this is the most important practical finding. |
| Build reproducibility | **Broken as published**: the compiled tree does not contain two files it imports, and stale duplicate sources sit at the repo root (§3.5). |

---

## 2. What the binary actually contains

| Item | APK | Note |
|---|---|---|
| Size / entries | 19.6 MB, 152 entries, 18.7 MB uncompressed | |
| DEX | `classes.dex` 12.3 MB + `classes2.dex` 5.9 MB | 2 DEX, **13,142 classes, 100,150 methods** — multidex, no packer (largest-DEX entropy 6.07, normal for unshrunk Kotlin) |
| Class mix | `androidx/compose` 7,276 · `androidx/core` 1,072 · `kotlinx/coroutines` 878 · `androidx/datastore` 569 · **`com/nova` 531** · `kotlinx/serialization` 395 · `okhttp3/internal` 214 | App code is ~4 % of the APK; the rest is library weight |
| Resources | `resources.arsc` 399 KB, 29 XML, 52 PNG | |
| Native libs | `libdatastore_shared_counter.so` only, for arm64-v8a / armeabi-v7a / x86 / x86_64 (4.4–7.1 KB each) | All stored uncompressed → correctly zipaligned for API 30+ |
| Assets | `english_words.txt` 44.9 KB, `sinhala_words.txt` 2.5 KB, `baseline.prof`/`.profm` | Matches the README's "6,000 English words + 166-word Sinhala starter list" |
| Deobfuscation | None — R8 minification is **off** (0.4 % short class names) | Consistent with the `v1.0.0` tag, which had `isMinifyEnabled = false` |
| AAB | modules `base`, `BUNDLE-METADATA`, `META-INF`; 2 DEX; protobuf `AndroidManifest.xml` decodes to the **same** package/version/SDK/permission set as the APK | Consistent release; single base module, no dynamic features |

### Network surface found in the code
Only one real endpoint is hard-coded: **`https://api.openai.com/v1`** (in `com.nova.keyboard.ai.AiClient`).
Everything else is framework noise (`schemas.android.com`, two Google bug-report URLs from Compose/DataStore).
A generic "OpenAI-compatible endpoint" feature means the effective host set is *whatever URL the user
types*, which is by design — but it is worth knowing that nothing restricts it to a domain allowlist.

### API-level behaviour worth noting (not vulnerabilities)
- `WebView` classes are referenced — expected from the AndroidX/Compose dependency graph, no `WebView`
  is instantiated by app code.
- Reflection (`Constructor/Field/Method`), `Cipher`, `KeyGenParameterSpec`, `ClipboardManager` and
  `Environment` are referenced — all consistent with the documented features (keystore-encrypted API key,
  clipboard manager, file export of themes/snippets). No `DexClassLoader`/`Runtime.exec` usage was found.

---

## 3. Findings

### 3.1 CRITICAL — the release signing key is published
`NOVA-KEYBOARD-release-keystore.jks` (4,388 B, PKCS#12, alias `nova`, 4096-bit RSA, SHA384withRSA,
self-signed, valid to 2056) is a **public release asset**, and `documentation/BUILD-GUIDE.md` publishes
its store password (`NovaRelease2026`).

Anyone can therefore produce an APK signed with the *same* certificate and identity as NOVA KEYBOARD.
Android treats "same signer" as "trusted update", so a modified build can be installed **over** the
genuine app (`adb install -r`, and any sideload path that does not compare versions) keeping all user
data, and can be published by third parties as an "update". Because the certificate is self-signed,
there is no revocation path; the only real remedy is to rotate the key (new certificate = new app
identity, so existing installs cannot be updated in place without an uninstall, or a documented
key-rotation migration using v3 proof-of-rotation from now on).

Actions:
1. Delete the `.jks` asset and the password from the release notes/docs (`BUILD-GUIDE.md` line 56).
2. Keep the key only in a secret store (GitHub Actions secrets / offline hardware token).
3. Treat the current certificate `bfffe869…` as burned for any high-trust distribution.
4. For future builds sign with **v2 + v3** (and rotate deliberately with proof-of-rotation).

### 3.2 LOW — APK Signature Scheme v2 only
`apksigner` reports v1 `false`, v2 `true`, v3/v3.1/v3.2/v4 `false`. That is fine for `minSdk 29`
(v1 is unnecessary), but **v3 is what carries key-rotation proof**, so today there is no way to rotate
the compromised key without breaking update continuity. Enabling v3 (and v4 for Play) costs nothing.

### 3.3 LOW — no network security config
There is no `networkSecurityConfig`, so there is no certificate-pinning or domain allowlist for the AI
endpoint. `usesCleartextTraffic="false"` already blocks plain HTTP, and OkHttp validates TLS normally —
so this is hardening, not a hole. If AI traffic is the only outbound path, a config that allowlists
`api.openai.com` + the user's configured host (or just pins the default) would shrink the blast radius.

### 3.4 HIGH (process) — the published APK is not the code in `main`
The release APK was signed with minification **off**, and its DEX carries no `com.nova.keyboard.core.CrashReporter`
class. In the repository, `3ae336e` ("fix: harden every startup path and record crashes in-app") adds
`CrashReporter`, wraps the IME lifecycle, adds `ServiceLocator.ensureInitialised()` and switches the
release build to R8. The release notes for `v1.0.0` predate that work, and the tag `v1.0.0` is the
last commit *before* `3ae336e`.

Consequence: the download on the Releases page does **not** contain the crash-safety fixes or the
in-app crash log, even though the repository's README/commit history implies it does. Users reporting
the "crash under investigation" on the release page are running the un-hardened build.

Action: rebuild from `3ae336e` (or later), re-sign (with a rotated key, see 3.1), and publish as
`v1.0.1` — or replace the `v1.0.0` assets explicitly, noting the checksum change.

### 3.5 HIGH (build) — the committed project does not compile, and stale sources shadow the real ones
- `app/src/main/java/.../ui/MainActivity.kt` and `.../core/NovaApplication.kt` **import
  `com.nova.keyboard.core.CrashReporter`**, but `CrashReporter` exists only at the repo root
  (`core/CrashReporter.kt`), outside any Gradle source set. `app/src/main/.../ime/TextEditing.kt` is
  missing too, yet `app/src/test/.../TextEditingTest.kt` imports `com.nova.keyboard.ime.TextEditing`.
  `./gradlew :app:assembleDebug` therefore fails with unresolved references.
- The repo root carries a second, **stale** copy of 13 Kotlin files (`core/`, `data/`, `ime/`, `theme/`,
  `ui/`). Every one differs from its `app/src/main/java/...` counterpart, e.g. root
  `core/NovaApplication.kt` still calls `ServiceLocator.init(this)` unguarded (14 lines vs 10). Editing
  the wrong copy is an easy mistake and makes review unreliable.

Actions: `git rm` the root-level duplicates (or move them into the source set), add the missing
`CrashReporter.kt` / `TextEditing.kt` under `app/src/main/java/com/nova/keyboard/…`, then prove the tree
builds (`./gradlew :app:assembleDebug :app:testDebugUnitTest`) in CI on every push. The analysis
workflow added here is a natural place to hang that.

### 3.6 INFO — small, non-blocking observations
- **Duplicated AAR metadata**: `META-INF` carries one `.version` file per library; normal.
- **Debug symbols**: the APK is not minified, so full class/method names and Kotlin metadata ship in
  the clear. Not a security hole (no secrets in code) but it makes reverse-engineering trivial and adds
  ~15 MB of dead weight that R8 would remove.
- **No `QUERY_ALL_PACKAGES`, no `SYSTEM_ALERT_WINDOW`, no `ACCESSIBILITY`** — good hygiene for an IME.
- **`DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION`** is a self-declared `signature` permission injected by
  AndroidX; expected.
- **AAB contains `kotlin-tooling-metadata.json` and `version-control-info.textproto`** — build metadata
  leaks the build environment (harmless, but removable if you want cleaner artefacts).

---

## 4. Reproducible toolchain (what was set up in this terminal)

`scripts/setup_apk_toolchain.sh` builds everything from reachable package indexes into `.toolchain/`
(git-ignored):

```bash
bash scripts/setup_apk_toolchain.sh      # JDK 17, aapt2, apktool, androguard, pyaxmlparser, apksigtool
source .toolchain/env.sh                 # java / aapt2 / apktool on PATH
```

Verified components: OpenJDK 17.0.9 (Temurin), aapt2 2.19-7832930, apktool 2.4.1, androguard 4.1.4.

Analysis run:

```bash
python3 scripts/apk_analysis.py --bin-dir <dir with NOVA-KEYBOARD.apk/.aab> --out apk-analysis \
        --aapt2 "$AAPT2" --apksigner "$(dirname "$AAPT2")/apksigner" --targets both
```

`scripts/apk_analysis.py` checks: package identity/SDK/badging, permissions, components and their
export/permission state, security flags, v1/v2/v3 signing + certificate details, DEX class/method
inventory, third-party SDK fingerprints, risky-API scan, hard-coded URL hosts, native libs, zipalign
state, packer entropy, duplicate entries, R8/minification evidence, keystore exposure, and APK-vs-AAB
consistency. It works on both APK (binary XML) and AAB (protobuf XML), and degrades with a recorded
reason when a tool is unavailable.

Not covered (needs a device/emulator, unavailable here): dynamic behaviour, runtime network calls,
on-device crash reproduction (`adb logcat`), Frida/instrumentation, and store-level checks.

---

## 5. Prioritised fix list

| # | Priority | Item |
|---|---|---|
| 1 | **Now** | Remove the `.jks` asset + password from the release/docs; assume the key is public. |
| 2 | **Now** | Publish a build that actually matches `main` (crash hardening + R8), as `v1.0.1`, with a rotated key and v2+v3 signatures. |
| 3 | High | Make the repo buildable: add `CrashReporter.kt` / `TextEditing.kt` to the source set, delete the 13 stale root copies. |
| 4 | High | Add CI that runs `assembleDebug` + unit tests on every push (the analysis workflow here is the template). |
| 5 | Medium | Enable `isMinifyEnabled`/`isShrinkResources` (already `true` on `main`) and ship the R8 build — cuts ~15 MB. |
| 6 | Low | Add a `networkSecurityConfig` pinning/allowlisting the AI endpoint; enable v3 signing. |
| 7 | Low | Strip `kotlin-tooling-metadata.json` / `version-control-info.textproto` from releases if build-environment privacy matters. |
