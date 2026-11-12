# NOVA KEYBOARD

**TYPE BEYOND LIMITS.**
AI POWERED. DEVELOPER READY. GAMING OPTIMISED. PRIVACY FIRST.

NOVA KEYBOARD is a native Android input method (IME) for Android 10+ with Sinhala and English
typing, AI writing tools, gaming and developer modes, a clipboard manager, a snippet and macro
system, and a full theme engine. It is a real `InputMethodService` — it types into other apps.

| | |
|---|---|
| Package | `com.nova.keyboard` |
| Version | 1.0.0 (versionCode 1) |
| Min SDK | 29 (Android 10) |
| Target SDK | 34 (Android 14) |
| Language | Kotlin 1.9.24, Jetpack Compose (settings app), custom Views (keyboard surface) |
| APK | **Releases** → `NOVA-KEYBOARD.apk` (signed, 19.6 MB) |
| AAB | **Releases** → `NOVA-KEYBOARD.aab` (signed, 6.8 MB) |

## What is inside

- **Keyboard engine** — QWERTY, Sinhala Wijesekara, Sinhala phonetic, numbers, two symbol pages,
  a programming page, emoji/kaomoji/symbol panels, long-press alternates, backspace auto-repeat,
  cursor keys, one-handed alignment, adjustable height and key spacing.
- **Sinhala + English** — a real Wijesekara mapping (SLS 1134 / Wij 9) with a syllable composer, a
  romanised (Singlish) transliterator with live candidates, and word prediction/autocorrection for
  both languages.
- **AI Studio** — rewrite, tone, grammar, translation (Sinhala ↔ English), summarise, reply and
  generation through any OpenAI-compatible endpoint you configure. Your key is encrypted with the
  Android Keystore. Nothing is ever sent without a tap.
- **Gaming mode** — gaming theme, larger keys, stronger haptics, phrase strip instead of predictions.
- **Developer mode** — programming symbol page, keyword rows for 15 languages, snippet manager
  (create/edit/delete/search/pin/import/export) and text-expansion macros.
- **Clipboard manager** — local history with pin, search, delete, limits and expiry.
- **Smart typing** — autocorrect, next-word prediction, personal dictionary, double-space full stop,
  auto-capitalisation, smart quotes and ellipsis.
- **Theme engine** — 10 genuinely distinct built-in themes plus a theme editor, duplicate, export and
  import.
- **Privacy** — password fields disable prediction, learning and AI; no keystroke logging; no trackers;
  every stored dataset is erasable from the Privacy centre.

## Download

Grab the signed APK from the **Releases** page. Install it, open NOVA, then enable it in Android keyboard
settings (see `documentation/INSTALLATION-GUIDE.md`).

## Build

```bash
./gradlew :app:assembleRelease      # signed APK  -> app/build/outputs/apk/release/
./gradlew :app:bundleRelease        # signed AAB  -> app/build/outputs/bundle/release/
./gradlew :app:testDebugUnitTest    # 30 unit tests
```

Full instructions, including SDK setup and signing, are in `documentation/BUILD-GUIDE.md`.

## Documentation

| File | Contents |
|---|---|
| `documentation/INSTALLATION-GUIDE.md` | Install the APK, enable the keyboard, first-run setup |
| `documentation/BUILD-GUIDE.md` | Toolchain, build commands, signing, release checklist |
| `documentation/ARCHITECTURE.md` | Module map, data flow, design decisions |
| `documentation/PRIVACY-POLICY.md` | What is stored, what is sent, what is never done |
| `documentation/SECURITY-REPORT.md` | Threat model and controls |
| `documentation/TEST-REPORT.md` | What was tested, how, and what was not |
| `documentation/THIRD-PARTY-LICENSES.md` | Every dependency and its licence |
| `documentation/FINAL-REPORT.md` | Requirement-by-requirement completion report |

## Honest status

The app compiles, the 30 unit tests pass, and the release APK is signed and verified
(`apksigner verify` → v2 scheme OK). It was **not** installed on a physical phone or emulator in
this build environment, so real-device typing in WhatsApp, browsers, code editors and so on has not
been exercised. `documentation/TEST-REPORT.md` lists exactly which requirements are verified by code,
which are implemented but unverified on hardware, and which are not implemented at all.
