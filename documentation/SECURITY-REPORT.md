# Security report

## Threat model

A keyboard is a privileged input path. The realistic threats are:

| Threat | Control |
|---|---|
| Silent keystroke harvesting | No keystroke buffer exists. Text goes from the key handler straight into `InputConnection`; nothing is persisted except optional word/bigram counts. |
| Password capture | `EditorTypeDetector` classifies password fields from `EditorInfo.inputType`; `isSensitive` disables prediction, learning, clipboard capture, smart punctuation and AI. The `SECURE` badge shows it is active. |
| Exfiltration of typed text | No network code runs unless the user taps an AI action. `AiClient` is the only network client in the app. |
| Credential theft from the device | AI key stored as AES-256-GCM ciphertext; the AES key is generated in the Android Keystore (`setKeySize(256)`, no export). Never logged, never in backups (`allowBackup=false`, extraction rules exclude prefs/db/files). |
| Credential theft from the APK | No key, token or endpoint credential is compiled in. `keystore.properties` is read at build time only; the signing keystore is delivered separately, not inside the source tree. |
| Plaintext transport | `android:usesCleartextTraffic="false"` and `AiClient` refuses any base URL that is not `https://`. |
| Malicious macros | Macros perform text insertion only. No shell, no `Intent`, no background action, no scripting surface. |
| Untrusted provider response | The response is treated as text and inserted only when the user taps Insert; nothing is executed or rendered as markup. |
| Over-broad permissions | INTERNET, ACCESS_NETWORK_STATE, VIBRATE only. |
| Clipboard leakage | History is local, skippable, expirable, and never captured in password fields. |
| Third-party code risk | Six small, well-known dependencies, all Apache 2.0 (see THIRD-PARTY-LICENSES.md). No analytics, ad or crash SDKs. |

## Verified in this build

- APK signature verifies (`apksigner verify` → APK Signature Scheme v2, RSA-4096, SHA-384 cert).
- Manifest declares `BIND_INPUT_METHOD` on the IME service and `exported=true` with that permission,
  which is the only correct way for an IME to be selectable.
- `usesCleartextTraffic=false` present in the merged manifest.
- No secret material in the source tree (`keystore.properties` is excluded from the source archive;
  `keystore.properties.example` contains placeholders only).
- Unit tests cover the pure logic that privacy depends on (transliteration, layouts, theme
  serialisation); the sensitive-field gate itself is in `EditorTypeDetector` + `KeyboardController`
  and needs a device to exercise end-to-end.

## Known limitations

- The sensitive-field decision trusts `EditorInfo.inputType`. Apps that type passwords into plain
  text fields cannot be detected; this is a platform-wide limitation for every keyboard.
- Clipboard capture depends on the OS granting clipboard access to the IME window; on Android 10+ a
  clipboard read can return nothing, in which case history simply stays empty.
- The AI key is only as safe as the device. On a rooted or compromised device, Keystore protection is
  reduced.
- `minifyEnabled` is off in this build, so the APK contains un-obfuscated bytecode. This does not
  weaken runtime security (there are no secrets in the code), but enabling R8 is recommended before a
  public store release.

## Reporting a vulnerability

Report privately to the developer contact; please do not open a public issue for anything that could
expose user text.
