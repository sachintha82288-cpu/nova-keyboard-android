# Test report

_Environment: Debian 12 sandbox, 2 vCPU, 2 GB RAM. JDK 17.0.13, Gradle 8.7, AGP 8.5.2, SDK 34._

## 1. Unit tests — 30 tests, 30 passing

Run with `./gradlew :app:testDebugUnitTest`.

| Suite | Tests | Covers |
|---|---|---|
| `SinhalaTransliteratorTest` | 6 | api → අපි, suba → සුබ, kohomada → කොහොමද, amma/ammaa, inherent vowel, anusvara, passthrough, candidate ordering |
| `WijesekaraComposerTest` | 7 | consonant+sign, kombuva-before-consonant reordering, `ෙ`+`ා`→ො, second `ා`→ෝ, hal conjuncts, syllable commit, base/shift tables |
| `SuggestionEngineTest` | 4 | edit distance: exact, single edit, bounded search, known-distance pairs |
| `LayoutTest` | 6 | QWERTY completeness, shift → uppercase, number row, Sinhala codepoints present, developer symbols and keywords, email `@` |
| `ThemeSerializationTest` | 3 | JSON round-trip, 10 unique themes, palettes genuinely differ |
| `MacroExpansionTest` | 1 | shortcut expansion contract |

Two real bugs were found by these tests during development and fixed:

1. A trailing `a` after a consonant produced an independent අ (`සුබ` came out `සුබඅ`). Fixed with the
   inherent-vowel rule.
2. `;email`-style macros never matched because the word extractor stripped the `;`. Fixed by matching
   macros against the raw token before the cursor.

## 2. Build verification — passed

| Check | Result |
|---|---|
| `:app:assembleDebug` | BUILD SUCCESSFUL |
| `:app:assembleRelease` | BUILD SUCCESSFUL, signed |
| `:app:bundleRelease` | BUILD SUCCESSFUL, signed AAB 6.8 MB |
| `aapt2 dump badging` | `package name='com.nova.keyboard' versionCode='1' versionName='1.0.0'`, `sdkVersion:'29'`, `targetSdkVersion:'34'`, label `NOVA KEYBOARD` |
| `apksigner verify` | **Verifies** — v2 scheme true, signer `CN=NOVA KEYBOARD`, RSA-4096 |
| Assets packaged | `assets/english_words.txt` (6,000 words), `assets/sinhala_words.txt` (166 words) present in the APK |
| IME declaration | `res/xml/method.xml` + service with `android.permission.BIND_INPUT_METHOD` in the merged manifest |

## 3. Not tested — and why

| Item | Status |
|---|---|
| Installing on a real phone | **Not performed.** No Android device or emulator is available in this build environment, and an emulator cannot run here (no KVM, 2 GB RAM). |
| Typing into WhatsApp, Telegram, Chrome, Gmail, Notes, code editors | **Not performed** for the same reason. |
| Password-field behaviour on device | **Not performed.** Implemented and reasoned about, unverified on hardware. |
| Instrumented tests (`androidTest`) | **Written as configuration only** — `androidx.test` is on the test classpath but no `androidTest` sources ship, because they could not be executed here. Deliberately not faked. |
| Performance targets (p95 < 50 ms key response, < 300 ms startup) | **Not measured.** These need a reference device. The design targets them (direct `Canvas` keys, cached dictionaries, no I/O on the touch path) but no number is claimed. |
| Rotation, low-memory, rapid typing stability | **Not measured.** |

## 4. What a device test should cover next

1. `adb install -r NOVA-KEYBOARD.apk`, enable the IME, select it.
2. Type English and Sinhala in three apps with different editors (a messaging app, a browser address
   bar, a code editor), including a multiline field.
3. Verify password fields show `SECURE` and produce no suggestions.
4. Verify email/URL/number fields open the right layout.
5. Long-press alternates, backspace repeat, cursor keys, one-handed alignment.
6. Emoji, clipboard, snippet and AI panels; insert from each.
7. Create a macro and confirm `;shortcut` + space expands.
8. Rotate, switch apps rapidly, kill and reopen the keyboard.
