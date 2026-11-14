# NOVA KEYBOARD 1.0.1 — audit report

_Date: 2026-11-14. Branch: `arena/7410c49f-nova-keyboard-android`._

This is what was actually found, fixed and verified in the 1.0.1 rebuild. Everything below was
reproduced before it was fixed; nothing in this list is "probably fine now".

## How this build was verified

| Step | Result |
|---|---|
| `:app:testDebugUnitTest` | **60 tests, 60 passing** (was: did not compile) |
| `:app:assembleDebug` | BUILD SUCCESSFUL |
| `:app:assembleRelease` | BUILD SUCCESSFUL, signed, aligned |
| `apksigner verify` / `aapt2 dump badging` / `zipalign -c` | run by the `Release` workflow on every `[release]` build |
| Published artefacts | `NOVA-KEYBOARD.apk`, `NOVA-KEYBOARD.aab`, `checksums.txt`, source and docs zips |

The runtime in this environment cannot reach Google Maven or the Gradle distribution, so the
canonical build path is the `Build` GitHub Actions workflow on this repository. It runs the unit
tests and assembles an APK on every push, and it writes its log to `.ci/last-build.log` so the
result is inspectable without the Actions UI.

## Severity 1 — the shipped tree did not build its tests

`app/src/test/java/com/nova/keyboard/TextEditingTest.kt` referenced
`com.nova.keyboard.ime.TextEditing`, but that file did not exist in the compiled source set
(`app/src/main/java/...`). The fixes it tests lived only in a stray top-level `ime/` folder that the
Gradle build never compiles.

That folder (`core/`, `data/`, `ime/`, `theme/`, `ui/`) was a duplicate copy of the source tree, and
in 13 files it was **newer** than the real one: `CrashReporter`, the startup guards in
`NovaInputMethodService`, the `CoroutineExceptionHandler`, the SQLite-3.22-portable upserts, the
macro snapshot cache, the theme decode cache, the emoji-search focus fix, and the crash-log panel in
About all existed there and were missing from the build. 1.0.0 shipped without them — which is why
"the source on `main` contains a crash reporter" was true but the APK did not.

**Fix:** the lost work is now in `app/src/main/java/...`, and the duplicate folders are deleted so
there is exactly one copy of every file.

## Severity 1 — Sinhala phonetic output was wrong for ordinary words

The transliterator produced Sinhala-looking text that reads as the wrong word. Reproduced against
`SinhalaTransliterator.transliterate` directly:

| Input | 1.0.0 | Correct | Cause |
|---|---|---|---|
| `thaththa` (father) | ථථ්ථ | තත්ත | `th`→ථ and `t`→ත were swapped relative to how Singlish is typed |
| `watak` (a bit) | වදක | වටක් | same swap, retroflex ට was unreachable from `t` |
| `ran` (gold) | රන | රන් | a word-final consonant kept its inherent vowel instead of getting al-lakuna |
| `sinhala` | සින්හල | සිංහල | no anusvara rule for `n` + `h` |
| `Hello` | ඃඑල්ලො | හෙල්ලො | capital `H` is the visarga code, so any capitalised word was mangled |
| `buddhaM` | බුද්ධම් | බුද්ධං | the case-folding rule ate the `M` anusvara code |
| `sri` / `shrii` | ස්රී / ශ්රී | ශ්‍රී | ශ්‍රී was never produced as one cluster |

**Fix:** the consonant table is corrected, a word-final hal rule and an anusvara rule were added,
case folding now preserves the `M`/`H`/`T`/`D`/`N` code letters, and `sri`/`shrii` are matched before
the consonant table.

**Deliberately not fixed:** `nd`, `mb`, `nj` clusters are still transliterated literally
(`Kandy` → කන්ද්‍ය). Singlish `nd` is genuinely ambiguous (න්ද *and* න්ඩ), and a wrong guess is worse
than a literal reading. Use the Wijesekara layer for exact spelling.

## Severity 1 — Wijesekara composer leaked stray marks

| Input | 1.0.0 | Correct |
|---|---|---|
| ක then `.` | ක. *(ok)* but ෙ then space | space + stray kombuva |
| ල then space then ක | ල SP ක *(ok)* | — |
| ෙ then space then ක | `SP` **ක** (the kombuva vanished from the composing buffer and was re-emitted after the space) | ෙ SP ක |

**Fix:** whitespace, digits and punctuation now close the syllable and commit in order, and a
pending pre-base kombuva is emitted as typed instead of being re-ordered across the space.

## Severity 2 — password detection was wrong in both directions

`EditorTypeDetector` compared the bare `TYPE_MASK_VARIATION` against text variation constants. A
numeric password (`TYPE_CLASS_NUMBER | 0x10`) was therefore **not** recognised as sensitive: no
`SECURE` badge, predictions and learning stayed on in a PIN field. The same comparison could flag
fields of other classes as passwords.

**Fix:** the variation is now matched inside its input class. Pinned by `EditorTypeDetectorTest`.

## Severity 2 — text corruption on ordinary typing

| Bug | Effect |
|---|---|
| Double-space period | Deleting one character *after* autocorrect had already rewritten the word turned `teh` into `the.. ` |
| Long-press alternates | `alternatesMode` was never cleared by space/enter, so the next space could insert an alternate |
| Emoji backspace | The surrogate-pair guard only ran for `count == 1`; backspace auto-repeat still split emoji |
| URL layout | A `.com` alternate inserted six literal characters into whatever domain was being typed, and the bottom row inserted a duplicate `.` where `/` belongs |
| `currentWord()` | Re-implemented `takeLastWhile { isLetter() }`, which stops at every Sinhala vowel sign, so prediction/autocorrect/learning only ever saw the last consonant |
| Shift | Stayed latched on the Sinhala layers: the layout never returned to the base row |

## Severity 2 — input state lost on every window show

`onWindowShown()` called `controller.onStartInput(currentInputEditorInfo)`, i.e. the full field
reset: the in-progress Sinhala syllable, the shift state, `lastWord` and the AI panel were all
discarded and the clipboard was re-read every time the IME window was re-shown. It now does a light
refresh and only resets when the editor actually changes.

## Severity 3 — stability and performance

- `KeyView`: backspace could delete twice on release (hardcoded 420 ms vs. a repeat that starts at
  420 ms). The repeat runnable now records that it ran.
- `AiClient`: `aiTimeoutSeconds` was a setting nothing read; every call now gets a `callTimeout`
  from it, and the base URL is parsed instead of `startsWith("https://")` (which accepted
  `https://` with no host).
- `SuggestionEngine.correct`: returned the first dictionary word within the edit distance, so `teh`
  could become any of tea/ten/… — wrong autocorrections are worse than none. It now requires a
  unique best candidate, resolved by frequency.
- `KeyboardController.onSpace` invalidated the suggestion cache on every space when learning was
  off, forcing a SQLite read on the next keystroke.
- `MacroRepository`: the in-memory snapshot was not invalidated by `seedIfEmpty`, so the four
  seeded macros were invisible until the process restarted.
- `NovaInputMethodService.onDestroy` now releases the root view and controller.
- `KeyboardRootView.setLayout(animate)` and an unused `clipboard` lookup in AI Studio removed.

## What is still not verified

Nothing in this report was exercised on a physical device or emulator: this environment has no
device and no KVM. Typing into WhatsApp/Chrome/Gmail, rotation, low-memory restart and real
password fields still need the checklist in `TEST-REPORT.md` §4. The keyboard *builds*, its 60 unit
tests pass, and the signed artefacts install over 1.0.0 — that is the guarantee, no more.
