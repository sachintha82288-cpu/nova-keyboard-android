# Installation guide

## 1. Install the APK

**On the phone (recommended)**

1. Copy `NOVA-KEYBOARD.apk` to the device (USB, Drive, email to yourself).
2. Open it with the Files app. Android asks to allow installs from this source — allow it for the
   Files app only.
3. Tap **Install**. The app appears as **NOVA KEYBOARD**.

**With adb**

```bash
adb install -r NOVA-KEYBOARD.apk
```

Minimum Android version is 10 (API 29). The APK is signed with the project release key, so updates
signed with the same key install over the top.

## 2. Enable the keyboard

1. Open **NOVA KEYBOARD** from the launcher.
2. Onboarding → **Continue** → **Open keyboard settings**.
3. In *Settings → System → Languages & input → On-screen keyboard → Manage keyboards*, turn on
   **NOVA KEYBOARD**. Android may show a warning about keyboards collecting text — that is the
   standard prompt for every third-party keyboard.
4. Back in NOVA, tap **Choose keyboard** and select **NOVA KEYBOARD**.
5. The dashboard now reads **Keyboard active**. NOVA only claims this after Android itself reports it.

## 3. First-run configuration

| Screen | What to set |
|---|---|
| Languages | English or Sinhala; Wijesekara or phonetic input |
| Themes | One of ten built-in themes, or edit your own |
| AI Studio | Optional. Paste an OpenAI-compatible base URL, model and API key |
| Gaming | Bigger keys, gaming theme, phrase strip |
| Developer | Programming language, snippets, macros |
| Privacy | Review what is stored; erase anything |

## 4. Using the keyboard

- **Globe** switches English ↔ Sinhala.
- **?123 / =\<** switch to numbers and symbols; **{ }** opens the programming page.
- **Long-press** a key for alternates (accents in English, ඳ ඟ ඦ ඬ ඹ and the AltGr letters in Sinhala).
- **Toolbar**: cursor left/right, AI, clipboard, snippets, emoji, gaming, developer, one-handed,
  settings, hide.
- **Sinhala phonetic**: type `amma`, `api`, `kohomada`, `suba`; the conversion previews as you type
  and candidates appear above the keys. Long vowels double the letter (`ammaa` → අම්මා). Tap a
  candidate or press space to commit.
- **Macros**: type `;email` then space to expand a shortcut you created.

## 5. Uninstall

Android Settings → Apps → NOVA KEYBOARD → Uninstall. Disable it as a keyboard first if you want the
system to fall back to another keyboard cleanly. Uninstalling deletes the app's private database
(learned words, clipboard history, snippets, macros).
