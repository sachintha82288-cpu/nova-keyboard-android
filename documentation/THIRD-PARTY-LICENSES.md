# Third-party licences

NOVA KEYBOARD is distributed with the components below. All are Apache 2.0, which permits commercial
distribution provided the licence and notices are retained.

| Component | Version | Licence | Use |
|---|---|---|---|
| Kotlin standard library | 1.9.24 | Apache 2.0 | Language runtime |
| AndroidX Core KTX | 1.13.1 | Apache 2.0 | Android extensions |
| AndroidX Activity Compose | 1.9.0 | Apache 2.0 | Compose host activity |
| Jetpack Compose (BOM 2024.06.00): ui, foundation, material3 | — | Apache 2.0 | Settings application UI |
| AndroidX Lifecycle Runtime Compose | 2.8.2 | Apache 2.0 | Compose lifecycle |
| Kotlin Coroutines (android) | 1.8.1 | Apache 2.0 | Background work |
| kotlinx.serialization JSON | 1.6.3 | Apache 2.0 | Settings and theme serialisation |
| AndroidX DataStore Preferences | 1.1.1 | Apache 2.0 | Settings storage |
| OkHttp | 4.12.0 | Apache 2.0 | AI provider HTTP client |
| JUnit 4 | 4.13.2 | Eclipse Public License 1.0 | Unit tests only, not shipped |
| AndroidX Test (ext-junit, runner) | 1.2.1 / 1.6.1 | Apache 2.0 | Instrumented tests, not shipped |

## Data and standards

| Item | Licence / basis |
|---|---|
| English word frequency list (`english_words.txt`) | Derived from the [wordfreq](https://github.com/rspeer/wordfreq) project (Apache 2.0) by taking the 6,000 most frequent alphabetic English words. |
| Sinhala starter word list (`sinhala_words.txt`) | Written for this project: 166 common Sinhala words in Sinhala script. |
| Wijesekara keyboard mapping | Follows the Wijesekara / Wij 9 arrangement standardised in **SLS 1134** (Sri Lanka Standards Institution), an open public standard. No proprietary layout file is copied. |
| Phonetic transliteration scheme | NOVA's own documented scheme (see `documentation/ARCHITECTURE.md`), not copied from a licensed IME. |
| Emoji, kaomoji, Unicode symbol lists | Unicode codepoints; Unicode data is used under the Unicode License. |
| Default snippets | Written for this project; every one is user-editable and deletable. |

## Fonts and images

The app ships no custom font files and no third-party artwork. The launcher icon is a vector drawn
for this project. Emoji are rendered by the device's system font, not bundled.

## Full licence text

Apache License 2.0: <https://www.apache.org/licenses/LICENSE-2.0>. Where a dependency's `NOTICE` file
is required, it is retained by the dependency's own packaging in the build output.
