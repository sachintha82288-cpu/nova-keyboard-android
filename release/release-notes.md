# NOVA KEYBOARD 1.0.0

First release. `versionCode 1`, `versionName 1.0.0`, minSdk 29, targetSdk 34.

## Highlights

- Real Android IME with English, Sinhala Wijesekara and Sinhala phonetic input.
- Sinhala syllable composer: `ෙ`+`ා` → ො, second `ා` → ෝ, hal conjuncts, AltGr letters as long-press.
- AI Studio with 14 writing actions over any OpenAI-compatible endpoint (your key, Keystore-encrypted).
- Gaming mode: theme, taller keys, phrase strip. Developer mode: 15 language layouts, snippets, macros.
- Clipboard history, 10 themes with a full editor, and a privacy centre with per-dataset deletion.

## Security and privacy

- No keystroke logging, no analytics, no trackers, no automatic uploads.
- Password fields disable prediction, learning, clipboard capture and AI (`SECURE` badge).
- HTTPS enforced; API key encrypted with AES-256-GCM under an Android Keystore key.
- `allowBackup=false`; nothing stored leaves app-private storage.

## Fixed during development

- Trailing `a` after a consonant produced an extra අ (`සුබ` → `සුබඅ`). Fixed with the inherent-vowel rule.
- `;shortcut` macros never matched because the word extractor stripped the `;`. Fixed by matching the
  raw token before the cursor.

## Known issues

- Not tested on a physical device in the build environment (see `documentation/TEST-REPORT.md`).
- Sinhala dictionary is a 166-word starter list plus learning.
- Floating keyboard and swipe typing are not implemented and are not advertised in the UI.

## Install

`adb install -r NOVA-KEYBOARD.apk`, then enable NOVA in Android keyboard settings.
