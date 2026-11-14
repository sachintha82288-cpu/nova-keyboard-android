# NOVA KEYBOARD 1.0.1

`versionCode 2`, `versionName 1.0.1`, minSdk 29, targetSdk 34. Installs over 1.0.0 (same
applicationId, same signing key).

This release replaces the unstable 1.0.0 build. See `documentation/AUDIT-1.0.1.md` for the full
list of bugs found, reproduced and fixed, and for exactly what was and was not verified.

## Highlights (unchanged from 1.0.0)

- Real Android IME with English, Sinhala Wijesekara and Sinhala phonetic input.
- AI Studio with 14 writing actions over any OpenAI-compatible endpoint (your key, Keystore-encrypted).
- Gaming mode, developer mode with 15 language layouts, snippets and macros.
- Clipboard history, 10 themes with an editor, and a privacy centre with per-dataset deletion.

## Fixed in 1.0.1

1. The build compiled a stale copy of 13 files — crash reporting, IME startup guards, the background
   crash handler, Android-10-safe SQLite upserts, the macro cache and the emoji-search focus fix were
   only in a duplicate folder the build ignored.
2. Sinhala phonetic output was wrong for ordinary words (`thaththa`, `watak`, `ran`, `sinhala`,
   `Hello`, `buddhaM`, `sri`), and the Wijesekara composer re-ordered a pending `ෙ` across spaces.
3. Numeric password fields were not treated as sensitive.
4. Double space produced `word.. `, long-press alternates leaked into the next word, backspace
   repeat split emoji, and the URL layout could insert a literal `.com`.
5. Every IME window show reset the in-progress syllable, shift state and AI panel.

## Verification

- 60 unit tests, 60 passing (`:app:testDebugUnitTest`) on a clean GitHub Actions runner.
- Debug APK assembles on every push; signed release APK + AAB are built by the `Release` workflow,
  which also runs `apksigner verify`, `aapt2 dump badging` and `zipalign -c`.

## Known limits

- Not tested on a physical device or emulator in this environment — see `documentation/TEST-REPORT.md`.
- The Sinhala dictionary is a starter list plus on-device learning.
- Floating keyboard and swipe typing are not implemented and are not advertised in the UI.

## Install

```bash
adb install -r NOVA-KEYBOARD.apk
```

Then open NOVA, enable it in *Android settings → System → Languages & input → On-screen keyboard →
Manage keyboards*, and select it.
