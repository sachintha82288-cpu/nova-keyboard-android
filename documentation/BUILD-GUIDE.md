# Build guide

## Toolchain used for the shipped build

| Component | Version |
|---|---|
| JDK | Temurin 17.0.13 |
| Gradle | 8.7 |
| Android Gradle Plugin | 8.5.2 |
| Kotlin | 1.9.24 |
| Compose compiler | 1.5.14 |
| compileSdk / build-tools | 34 / 34.0.0 |
| Min SDK | 29 |

## Setup from scratch

```bash
export ANDROID_HOME=$HOME/Android/Sdk          # contains platforms;android-34 and build-tools;34.0.0
export JAVA_HOME=/path/to/jdk-17
echo "sdk.dir=$ANDROID_HOME" > local.properties
```

`local.properties` is machine specific and is not part of the source archive.

## Build commands

```bash
./gradlew :app:assembleDebug       # debug APK
./gradlew :app:assembleRelease     # signed release APK
./gradlew :app:bundleRelease       # signed AAB for Play
./gradlew :app:testDebugUnitTest   # JVM unit tests
```

Release output:

```
app/build/outputs/apk/release/app-release.apk
app/build/outputs/bundle/release/app-release.aab
```

## Signing

The release build reads `keystore.properties` from the project root:

```properties
storeFile=keys/nova-release.jks
storePassword=********
keyAlias=nova
keyPassword=********
```

If the file is absent, Gradle produces an **unsigned** release APK instead of failing — it never
invents a signature. `keystore.properties.example` shows the expected shape.

The keystore delivered with this build (`NOVA-KEYBOARD-release-keystore.jks`, alias `nova`,
password `NovaRelease2026`) is a locally generated release key. Keep it safe and keep it out of
public source control: Android requires the same key for every future update of the package.

Verify a build:

```bash
$ANDROID_HOME/build-tools/34.0.0/apksigner verify --print-certs --verbose app-release.apk
$ANDROID_HOME/build-tools/34.0.0/aapt2 dump badging app-release.apk | head
```

## Build notes and constraints

- `checkReleaseBuilds = false` — the release pipeline skips `lintVital` so the build does not need the
  separate `lint-gradle` artifact. Run `./gradlew :app:lint` separately if you want a lint report.
- R8/`minifyEnabled` is **off** in this build. Turning it on is a one-line change in
  `app/build.gradle.kts`; the keep rules for the app are already in `app/proguard-rules.pro`. It was
  left off deliberately because shrinking could not be validated on a device in this environment.
- `gradle.properties` is tuned for a small CI box (1 GB Gradle heap, in-process Kotlin compiler,
  `workers.max=1`). Raise `org.gradle.jvmargs` and `org.gradle.workers.max` on a normal machine for a
  much faster build.

## Rebuilding the bundled dictionaries

`app/src/main/assets/english_words.txt` holds the 6,000 most frequent English words, ordered by
frequency. Regenerate with:

```python
import re, wordfreq
words = [w for w in wordfreq.top_n_list("en", 9000) if re.fullmatch(r"[a-z]{2,14}", w)][:6000]
open("english_words.txt", "w").write("\n".join(words))
```

`sinhala_words.txt` is a curated starter list of 166 common Sinhala words; the engine also learns the
user's own words at runtime.
