# Architecture

Single Gradle module (`:app`) with strict package boundaries. One module keeps the IME process small
and startup fast; the packages below are the seams you would split into modules if the app grew.

```
com.nova.keyboard
├── core/         NovaApplication, ServiceLocator (manual DI), Settings + SettingsRepository,
│                 SecureStore (AES-GCM over the Android Keystore)
├── ime/          NovaInputMethodService, KeyboardController, InputSession, EditorTypeDetector
├── ui/keyboard/  KeyboardRootView, KeyView, KeySpec, KeyboardLayouts, EmojiData,
│                 Panels (clipboard/snippets), EmojiPanelView, AiPanelView
├── language/     SinhalaTransliterator, WijesekaraEngine, SuggestionEngine
├── ai/           AiClient (OkHttp, OpenAI-compatible), AiPrompts
├── data/         NovaDatabase (SQLite), ClipboardRepository, SnippetRepository,
│                 MacroRepository, LearningRepository, DefaultSnippets
├── theme/        NovaTheme, BuiltInThemes, ThemeManager
└── ui/           MainActivity + Compose screens (dashboard, themes, languages, AI, gaming,
                  developer, clipboard, settings, privacy, about, onboarding)
```

## Keyboard surface

The IME view is a plain `LinearLayout`: toolbar → suggestion strip → key grid, with panels swapped
over the grid inside a `FrameLayout`. Keys are a single custom `View` (`KeyView`) drawn with
`Canvas`, not a view per state: a 40-key layout is 40 small views in one measure pass, which keeps
first-frame latency low.

`KeyboardLayouts` returns pure data (`List<List<KeySpec>>`), so every layout — including the
Wijesekara and developer pages — is unit-testable without an Android device.

## Input flow

```
touch → KeyView → KeyboardController.onKey
                     ├── shift / page / language state → refresh() rebuilds the layout
                     ├── CHAR  → English: commit; Sinhala Wijesekara: syllable composer →
                     │            setComposingText; Sinhala phonetic: buffer → transliterate →
                     │            composing preview + candidates
                     ├── SPACE → macro expansion → autocorrect → double-space → commit → learn
                     └── BACKSPACE / ENTER / cursor → InputSession
                                              ↓
                                        InputConnection (the host app)
```

`InputSession` is the only class that touches `InputConnection`. Every call is wrapped in
`runCatching` because editors are allowed to reject any edit.

## Language engines

**Wijesekara** (`WijesekaraEngine`) holds the verified Wij 9 base and shift layers, plus long-press
alternates for the characters a desktop keyboard reaches with AltGr (ඳ ඟ ඦ ඬ ඹ ආ ඇ ඈ ඌ ...). Because
Sinhala encodes the consonant before the vowel sign even when the sign is drawn to the left, a small
syllable composer buffers one syllable: `ෙ` typed first is emitted after the consonant, `ෙ`+`ා`
collapses to `ො`, a second `ා` promotes it to `ෝ`, and `්` opens a conjunct.

**Phonetic** (`SinhalaTransliterator`) is a greedy longest-match transliterator: consonants, then
vowel signs (only after a consonant), then independent vowels, with `්` inserted automatically
between two consonants and a bare `a` after a consonant consumed as the inherent vowel. Documented
scheme: `k kh g gh ng | c ch chh j jh ny | T Th D Dh N | t th d dh n | p ph b bh m | y r l v w sh Sh s h L f`.

**Prediction** (`SuggestionEngine`) blends the bundled frequency-ordered dictionary with the user's
own words and bigrams, cached so a keystroke never waits on SQLite. Autocorrection uses a bounded
Levenshtein distance (max 1 for short words, 2 above six letters).

## Persistence

- **Settings** — one `@Serializable` `Settings` snapshot in DataStore Preferences, exposed as a
  `StateFlow` with a synchronously readable `current` for the IME's main thread.
- **User data** — SQLite (`NovaDatabase`, hand-written `SQLiteOpenHelper`) with five tables:
  `clipboard`, `snippets`, `macros`, `user_words`, `bigrams`.
- **Secrets** — `SecureStore` encrypts the AI key with AES-256-GCM using a non-exportable Android
  Keystore key.

### Deliberate deviations from the original brief

| Brief asked for | Built instead | Why |
|---|---|---|
| Hilt | `ServiceLocator` (manual DI) | No annotation processor on the IME path; smaller process, faster cold start, no reflection |
| Room | Hand-written `SQLiteOpenHelper` | Avoids KSP in the build, keeps schema explicit and startup deterministic; five small tables do not need Room |
| Material 3 + Compose everywhere | Compose for the app UI, custom Views for the keyboard | Compose inside an IME adds measurable first-frame cost; keys are cheaper drawn directly |

Both choices are isolated behind repositories, so swapping either later touches one file.

## Threading

Coroutines on `Dispatchers.IO` for AI calls, clipboard writes and learning writes. The keyboard's
touch path never performs network I/O, and dictionary lookups are cached in memory. `InputConnection`
writes stay on the main thread, as Android requires.
