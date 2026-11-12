# NOVA KEYBOARD privacy policy

_Last updated: 12 November 2026 · Applies to NOVA KEYBOARD 1.0.0 (`com.nova.keyboard`)_

A keyboard sees almost everything you type, so this policy describes exactly what the app does,
because it was written from the code, not around it.

## Summary

- NOVA does **not** collect, log or upload your keystrokes.
- NOVA does **not** contain analytics, advertising or crash-reporting SDKs.
- NOVA works fully offline. The only network requests are AI requests **you** start.
- Everything NOVA stores stays in the app's private storage on your device and is erasable in-app.

## What is stored on your device

| Data | Where | Why | Erased by |
|---|---|---|---|
| Settings and theme | DataStore, app-private | Remember your configuration | Reset all settings |
| Learned words and word pairs | SQLite `user_words`, `bigrams` | Better predictions for how *you* write | Privacy centre → Erase learned words |
| Clipboard history | SQLite `clipboard` | Paste something you copied earlier | Privacy centre → Clear clipboard history |
| Snippets and macros | SQLite `snippets`, `macros` | Insert reusable text | Privacy centre → Delete snippets and macros |
| AI provider key | AES-256-GCM ciphertext, key in Android Keystore | Authenticate your AI requests | AI Studio → Remove API key |

Nothing is stored in shared storage, and `allowBackup=false` plus the data-extraction rules keep all
of it out of cloud backups.

## What leaves your device

Only AI requests you explicitly trigger, and only the text you selected (or the text immediately
before the cursor) — sent over HTTPS to the provider **you** configured, authenticated with **your**
key. NOVA never sends text on its own, on a timer, or in the background.

## What NOVA never does

1. Never records a keystroke history.
2. Never uploads typed text automatically.
3. Never logs or stores passwords, and never exposes password-field text to predictions, learning,
   clipboard capture or AI.
4. Never reads the clipboard in the background — capture happens only while the keyboard is shown and
   is skipped in password fields.
5. Never uses hidden background services, wake locks or trackers.
6. Never bundles an API key or secret in the APK.
7. Never makes a privacy claim the code does not implement.

## Sensitive fields

When Android reports a password input type (`TYPE_TEXT_VARIATION_PASSWORD`,
`TYPE_TEXT_VARIATION_VISIBLE_PASSWORD`, `TYPE_TEXT_VARIATION_WEB_PASSWORD`, or the numeric password
variation), NOVA switches to secure mode: no suggestions, no autocorrection, no learning, no clipboard
capture, no AI, no smart punctuation. The toolbar shows `SECURE`. If the input type is ambiguous NOVA
takes the safer path.

## Permissions and why

| Permission | Reason |
|---|---|
| `INTERNET` | AI requests you start |
| `ACCESS_NETWORK_STATE` | Report "network unavailable" instead of hanging |
| `VIBRATE` | Haptic key feedback |

No contacts, storage, location, camera, microphone, SMS or accessibility permissions are requested.

## Children

NOVA is a general-purpose keyboard and is not directed at children. It collects no personal data,
which is the strongest protection it can offer.

## Your rights

Every dataset above can be inspected and deleted from the Privacy centre inside the app. Uninstalling
the app removes all of it from the device. Because NOVA holds no server and no account, there is no
remote copy to request or delete.

## Contact

Privacy questions: the developer contact listed on the app's store page / repository.
