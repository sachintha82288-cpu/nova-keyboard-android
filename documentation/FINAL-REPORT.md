# NOVA KEYBOARD — final completion report

**Version 1.0.0 (versionCode 1) · package `com.nova.keyboard` · built 12 November 2026**

This report maps the 21-part master brief to what the shipped build actually does. Three statuses are
used and nothing is upgraded without evidence:

- **VERIFIED** — exercised by the build or by a passing unit test in this environment.
- **IMPLEMENTED** — real code, compiled into the APK, but not exercised on hardware here.
- **NOT IMPLEMENTED** — absent, with the reason stated.

## 1. Product overview

NOVA KEYBOARD is a native Android IME for Android 10+ with Sinhala and English input, AI writing
tools, gaming and developer modes, clipboard history, snippets/macros and a full theme engine.
Brand line: *TYPE BEYOND LIMITS. AI POWERED. DEVELOPER READY. GAMING OPTIMISED. PRIVACY FIRST.*
The design is original (dark AMOLED surfaces, neon accents, glass-style keys); no commercial
keyboard's UI is copied.

## 2. Architecture

Single module, 8 packages: 40 Kotlin source files (5,186 lines) in `app/src/main` plus 6 test files (282 lines) in `app/src/test`. See
`ARCHITECTURE.md` for the module map, the input flow and the three deliberate deviations from the
brief (manual DI instead of Hilt, hand-written SQLite instead of Room, custom Views instead of Compose
for the key surface).

## 3. Feature list

| Area | Status | Detail |
|---|---|---|
| Real IME (`InputMethodService`, `InputConnection`, `EditorInfo`) | IMPLEMENTED | `NovaInputMethodService`, registered with `BIND_INPUT_METHOD`, `res/xml/method.xml`, two subtypes (en_US, si_LK). Appears in Android keyboard settings. |
| QWERTY, shift/caps, backspace with auto-repeat, space, enter, numbers, two symbol pages | IMPLEMENTED | Every key has a handler; no dead buttons. |
| Long-press alternates | IMPLEMENTED | Accents for English; ඳ ඟ ඦ ඬ ඹ ආ ඇ ඈ ඌ and the AltGr letters for Sinhala; symbols for `-`, `.`, `?`. |
| Toolbar, suggestions strip, cursor keys, one-handed alignment, adjustable height/spacing | IMPLEMENTED | One-handed uses width + padding alignment (not a floating window). |
| Language switching with indicator | IMPLEMENTED | Globe key, `EN` / `SI` / `SI-PH` badge. |
| Sinhala Wijesekara (Wij 9) layout | VERIFIED (mapping + composer) | Base and shift layers from the SLS 1134 / Wij 9 arrangement; syllable composer tested. |
| Sinhala phonetic (Singlish) | VERIFIED (engine) | Greedy transliterator with live composing preview and candidates. |
| English suggestions, autocorrection, personal dictionary | VERIFIED (algorithm) / IMPLEMENTED (UI) | 6,000-word frequency dictionary + learned words; bounded Levenshtein correction. |
| Next-word prediction | IMPLEMENTED | Learned bigrams, cached; no cross-device model. |
| Smart punctuation, double-space full stop, auto-capitalisation | IMPLEMENTED | Quotes, apostrophes, `...`→`…`, sentence-start capital. |
| Emoji / kaomoji / Unicode symbols with search | IMPLEMENTED | 196 emoji with keywords, 16 kaomoji, 56 symbols, live search field, categories. |
| Clipboard manager | IMPLEMENTED | SQLite history, pin, search, delete, clear, limit, expiry, capture while shown, skipped in password fields. |
| Snippet manager | IMPLEMENTED | Create/edit/delete/search/pin/insert, 12 seeded snippets, import/export. |
| Macros | IMPLEMENTED | Shortcut → text expansion on space; text insertion only. |
| Developer mode | IMPLEMENTED | Programming page with `{ } [ ] ( ) < > = == != + - * / % && \|\| ! -> => ; : , . # $ @ _ \\ \| \` ~`, keyword rows for 15 languages, plus a quick-symbol row on the letters page. |
| Gaming mode | IMPLEMENTED | Gaming theme, 8% taller keys, stronger haptics, phrase strip replacing predictions, gaming phrases insertable. |
| AI: rewrite/tone, grammar, translation, summarise, reply, generate | IMPLEMENTED | Real OpenAI-compatible `/chat/completions` calls with the user's own key. 14 actions in the AI panel. |
| 10 built-in themes + editor + duplicate/export/import/reset | VERIFIED (uniqueness, serialisation) / IMPLEMENTED (editor) | Ten distinct palettes; editor adjusts background, key, text, accent colours, radius, border, transparency. |
| Settings app: onboarding, dashboard, 9 module screens | IMPLEMENTED | Compose UI, live keyboard-status check, backup/restore of settings via clipboard. |
| Privacy centre | IMPLEMENTED | Storage inventory with live counts, toggles, per-dataset deletion. |
| Password/sensitive-field handling | IMPLEMENTED | `SECURE` badge; predictions, learning, clipboard capture, smart punctuation and AI all off. |

## 4. Sinhala engine details

- **Wijesekara**: base layer `q=ු w=අ e=ැ r=ර t=එ y=හ u=ම i=ස o=ද p=ච a=් s=ි d=ා f=ෙ g=ට h=ය j=ව
  k=න l=ක ;=ත z=‘ x=ං c=ජ v=ඩ b=ඉ n=බ m=ප ,=ල .=ග`; shift layer `Q=ූ W=උ E=ෑ R=ඍ T=ඔ Y=ශ U=ඹ I=ෂ
  O=ධ P=ඡ A=ෟ S=ී D=ෘ F=ෆ G=ඨ H=්‍ය J=ළු K=ණ L=ඛ :=ථ`; AltGr characters as long-press alternates.
- **Composer rules**: `ෙ` typed before the consonant is emitted after it (logical order); `ෙ`+`ා` → `ො`;
  a second `ා` → `ෝ`; `්` before a consonant forms a conjunct; a new consonant commits the previous
  syllable. All covered by tests.
- **Phonetic**: documented longest-match scheme; `api→අපි`, `suba→සුබ`, `kohomada→කොහොමද`,
  `amma→අම්ම`, `ammaa→අම්මා`, `shrii→ශ්‍රී`. Candidates include dictionary words sharing the prefix.
- **Dictionary**: 166 bundled Sinhala words plus runtime learning. Honest limitation: this is a starter
  list, not a corpus-scale lexicon, so Sinhala word prediction is much thinner than English.

## 5. AI integration details

Provider-based: base URL + model + key, all user supplied. Key encrypted with AES-256-GCM under a
non-exportable Android Keystore key. HTTPS enforced (`AiClient` rejects non-`https://` URLs).
Timeouts 15 s connect / 90 s read. Errors are surfaced verbatim ("Provider error: …", "Network
unavailable…"). No hardcoded responses, no fallback fake text. 14 actions: improve, shorter, longer,
professional, casual, friendly, formal, grammar, EN→SI, SI→EN, summarise, reply, email, caption.
Requires the user's own key; without one the panel explains how to add it and the keyboard keeps
working.

## 6. Gaming features

Gaming theme, taller keys, strong haptics, phrase strip (15 phrases), gaming mode toggle that changes
the actual keyboard configuration. Explicitly **not** claimed: aim assistance, input injection,
automation inside games, anti-cheat bypass. This is stated in the Gaming screen itself.

## 7. Developer features

15 language presets (kotlin, java, javascript, typescript, python, html, css, php, c, cpp, csharp,
sql, json, xml, bash) each with a symbol page and a keyword row. Snippet CRUD with pin, search,
import/export and 12 seeded templates. Macro CRUD with expansion on space. Import/export formats are
documented in code (`#language|title` blocks).

## 8. Theme customisation

Ten built-in themes (AMOLED Cyber, Neon Fusion, Liquid Glass, Stealth Black, Midnight Purple, Arctic
Blue, Crimson Dark, Minimal White, Developer Terminal, Gaming RGB) with distinct backgrounds, key
colours, accents, radii, borders, alpha and glow. Editor covers background, key, text and accent
colour, key radius, border width and transparency. Duplicate, delete, export to clipboard, import from
clipboard, reset. All work offline.

## 9. Privacy and security

See `PRIVACY-POLICY.md` and `SECURITY-REPORT.md`. Headline: no keystroke logging, no automatic
uploads, no password capture, HTTPS only, no secrets in the APK, three permissions, every dataset
erasable in-app, backups disabled.

## 10. Testing results

30 unit tests, 30 passing (6 suites). Build verification: debug + release APK + AAB all build; APK
signed and verified with the v2 scheme; badging confirms package, version, minSdk 29, targetSdk 34.
**No device or emulator test was possible in this environment** — see `TEST-REPORT.md` §3 for the full
list of what was not run.

## 11. Known limitations

1. **No on-device verification.** The single biggest caveat: nothing was typed on real hardware here.
2. **Floating keyboard not implemented.** One-handed alignment is provided instead.
3. **Swipe/glide typing not implemented** — and therefore never claimed anywhere in the UI.
4. **Key-preview popup not implemented** (the setting was removed rather than shipped dead).
5. **Sinhala dictionary is small** (166 words + learning).
6. **Sinhala typing needs the *user's* choice of method**; automatic language detection is not
   implemented (manual globe switching only).
7. **R8/minification off** in the release build (documented in `BUILD-GUIDE.md`).
8. **No instrumented tests** were executed.
9. **Undo/redo** is not exposed; only cut/copy/paste/select-all primitives are available through the
   editor.
10. **AI features need a third-party key**; without one they are inert by design, not broken.

## 12. Build information

| | |
|---|---|
| Gradle / AGP / Kotlin | 8.7 / 8.5.2 / 1.9.24 |
| JDK | Temurin 17.0.13 |
| compileSdk / build-tools | 34 / 34.0.0 |
| minSdk / targetSdk | 29 / 34 |
| Signing | RSA-4096, SHA-384 cert `CN=NOVA KEYBOARD`, APK Signature Scheme v2 |
| Certificate SHA-256 | `bfffe869298e31e74dabe7923f6974b075e4ce6b73f5a7da7190e68819343a9b` |

## 13–14. Artifacts

| File | Size |
|---|---|
| `release/NOVA-KEYBOARD.apk` | 19.6 MB |
| `release/NOVA-KEYBOARD.aab` | 6.8 MB |
| `release/checksums.txt` | SHA-256 of both |

## 15. Installation

See `INSTALLATION-GUIDE.md`. Short version: install the APK, open the app, enable NOVA in Android's
keyboard settings, select it, then type.
