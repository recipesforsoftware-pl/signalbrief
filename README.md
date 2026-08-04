# SignalBrief

SignalBrief is a production-oriented Android news reader that is being migrated
incrementally to Kotlin Multiplatform. This repository currently contains a
verified **Android app backed by a shared Kotlin Multiplatform domain module**
(`:shared`); the Android-only baseline is described in the history. The product
direction — an Android and iOS application with offline-first reading, saved
articles, search, topic monitoring, and a Daily Brief — is defined in the
[implementation roadmap](IMPLEMENTATION_ROADMAP.md) and is **future work**.

## Current status

- **Platforms:** Android app (`:app`) plus a Kotlin Multiplatform shared module
  (`:shared`) that targets Android, iOS, and JVM. The clean domain boundary
  lives in `commonMain`; there is **no iOS app, no iOS network layer, and no
  Compose Multiplatform UI yet** — the iOS targets currently build the shared
  framework (`SignalBriefShared`) and run common tests on the simulator.
- **Scope:** the app renders a "Top Headlines" feed from NewsAPI in one Compose
  screen with light and dark theme support. The Android app consumes the shared
  domain models and contract; the data layer, Hilt wiring, and UI remain in
  `:app`.
- **Not yet implemented:** Compose Multiplatform, an iOS app, offline-first
  storage, navigation, search, saved articles, topic monitoring, the Daily
  Brief, payments, synchronization, and a production backend. See the roadmap
  for the planned work.

## Implemented features

- Fetches and displays US top headlines from NewsAPI.
- Loading state, error state with retry, and success list rendering.
- Refresh action in the top app bar.
- Article cards open the original article in Chrome Custom Tabs.
- Material 3 theme with dynamic color support (Android 12+) and a persisted
  dark-mode toggle backed by DataStore.

## Architecture

MVVM with a single-activity Compose UI and a clean domain boundary. The domain
boundary now lives in the shared KMP module (`commonMain`) and is reused by the
Android app:

```
UI (Compose, :app)  ->  TopHeadlineViewModel (StateFlow + sealed UiState)  ->  NewsRepository (domain contract, :shared)  ->  TopHeadlineRepository (:app)  ->  Retrofit (NewsAPI)
                                                                                                                          |
                                                              ArticleDto --ArticleMapper--> Article (domain model, :shared)
                                                                                                                          |
                                                       ThemeViewModel <-> ThemePreference (DataStore)
```

- One launcher activity (`TopHeadlineActivity`).
- `TopHeadlineViewModel` exposes a `StateFlow<UiState>` with sealed
  `Loading` / `Success` / `Error` states and consumes the `NewsRepository`
  domain contract.
- `NewsRepository` (domain contract), `Article`/`Source` (domain models), and
  `NewsFailure` (typed failures) live in `:shared:commonMain`. They are
  framework-independent: no Retrofit, Gson, Android, Hilt, or transport DTO
  imports.
- `TopHeadlineRepository` (data layer, `:app`) is the only code touching
  Retrofit/Gson. It maps `ArticleDto` to the domain `Article` and translates
  transport exceptions into typed `NewsFailure` values (`Network`,
  `InvalidData`, `Unknown`). Cancellation is always rethrown.
- Hilt binds `NewsRepository` to `TopHeadlineRepository` in `RepositoryModule`.
- A `ThemePreference` (DataStore) plus `ThemeViewModel` persist and expose the
  dark-mode setting.

This is the current, honest state of the code. The target architecture (shared
data and presentation layers for Android and iOS) is described in the
[implementation roadmap](IMPLEMENTATION_ROADMAP.md).

## Technology stack

| Category | Technology |
|---|---|
| Language | Kotlin, Kotlin Multiplatform |
| UI | Jetpack Compose, Material 3 (Android app) |
| Architecture | MVVM with StateFlow |
| Dependency injection | Dagger/Hilt |
| Networking | Retrofit + Gson |
| Image loading | Coil 3 |
| Persistence | DataStore Preferences |
| Async | Coroutines + Flow |
| Browser | Chrome Custom Tabs |
| Build | Gradle wrapper, AGP |
| Unit testing | JUnit 4, MockK, Turbine, kotlinx-coroutines-test, Robolectric, kotlin.test |
| UI testing | Compose UI test, Espresso |

## Project structure

```
app/
├── build.gradle.kts
└── src/
    ├── main/java/com/recipesforsoftware/mvvm/
    │   ├── NewsApplication.kt            # @HiltAndroidApp
    │   ├── data/
    │   │   ├── api/NetworkService.kt     # Retrofit API interface
    │   │   ├── remote/dto/               # Gson DTOs (ArticleDto, SourceDto, TopHeadlinesResponseDto)
    │   │   ├── remote/mapper/            # DTO -> domain mapping (ArticleMapper)
    │   │   └── repository/TopHeadlineRepository.kt  # NewsRepository implementation (Retrofit)
    │   ├── di/                           # Hilt network + repository modules and qualifiers
    │   ├── ui/
    │   │   ├── base/UiState.kt
    │   │   ├── components/ArticleCard.kt
    │   │   ├── screens/TopHeadlineScreen.kt
    │   │   ├── theme/                    # Color, Theme, Type, ThemePreference, ThemeViewModel
    │   │   └── topheadline/              # TopHeadlineActivity, TopHeadlineViewModel
    │   └── utils/AppConstant.kt
    ├── test/                             # JVM unit tests
    └── androidTest/                      # Instrumented (device) tests
shared/
├── build.gradle.kts                      # KMP: android + iosArm64 + iosSimulatorArm64 + iosX64
└── src/
    ├── commonMain/kotlin/com/recipesforsoftware/mvvm/domain/
    │   ├── failure/NewsFailure.kt        # Typed failures: Network, InvalidData, Unknown
    │   ├── model/                        # Domain models (Article, Source)
    │   └── repository/NewsRepository.kt  # Domain contract
    └── commonTest/kotlin/com/recipesforsoftware/mvvm/domain/
        └── ...                           # Common tests (models, failures, repository contract)
├── gradle/libs.versions.toml             # Version catalog
└── settings.gradle.kts                   # Root project name: SignalBrief
```

## Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/recipesforsoftware-pl/signalbrief.git
   ```
2. Open the project in Android Studio (or build from the command line).
3. Get a NewsAPI developer key from [NewsAPI.org](https://newsapi.org/register).
4. Store the key in the local, ignored `local.properties` file (see below).

### API key handling and security limitations

The current development mode reads a user-supplied NewsAPI key from the
**ignored** `local.properties` file:

```properties
NEWS_API_KEY=your_news_api_key
```

Notes:

- `local.properties` must **never be committed**. It is listed in `.gitignore`.
- The key is embedded into `BuildConfig` at build time and sent in the
  `X-Api-Key` header. A client-side key is extractable from the APK, so this
  setup is intended for **local development only**, never as a secure production
  credential.
- You are responsible for complying with the selected data provider's licensing
  and usage terms (for example NewsAPI's developer-tier restrictions and rate
  limits).

### Development mode (currently available)

The only currently available mode is the local development mode described above:
the user provides their own NewsAPI key and runs the app locally. No real key is
included in this repository or required by CI.

### Production-connected mode (planned, not implemented)

A future production-connected mode is planned around an **authorized production
data provider** and, where required, a **backend proxy** so that credentials are
never embedded in the client. This mode is **not implemented in this baseline**,
and no deterministic demo fixtures exist yet. Deterministic demo data is planned
as later work so the app can run and be tested without a third-party key.

## Running the app

```bash
# Debug build
./gradlew assembleDebug

# Install on a connected device/emulator
./gradlew installDebug
```

Without a valid key in `local.properties`, the app builds and runs but headline
requests fail — the development key is required to see live data.

## Testing

```bash
# Android app JVM unit tests
./gradlew test

# Shared KMP module: JVM host tests + iOS simulator tests + iOS framework link
./gradlew :shared:allTests
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64

# Instrumented tests (require a connected device or emulator)
./gradlew connectedDebugAndroidTest
```

- **Android unit tests** cover the DTO-to-domain mapper (happy path and
  missing/invalid remote data), the repository (success, typed failure mapping,
  cancellation propagation), the top-headlines ViewModel
  (loading/success/typed-error flows), and the theme ViewModel (dark-mode
  persistence).
- **Shared common tests** cover the domain models (`Article` value semantics),
  the typed failures (`NewsFailure`), and the `NewsRepository` contract. They
  run both on the JVM (`:shared:testAndroidHostTest`) and on the iOS simulator
  (`:shared:iosSimulatorArm64Test`). On arm64 Macs the x86_64 simulator target
  (`iosX64Test`) is automatically skipped because no x86_64 simulator runtime is
  installed.
- **Instrumented tests** cover the top-headlines Compose screen, DataStore
  theme-preference behavior, and application package verification. They exist in
  `app/src/androidTest` but are **not** part of the CI workflow yet; executing
  them requires a device or emulator.

## Quality gates

The following commands pass on the current state:

```bash
./gradlew ktlintCheck
./gradlew detekt
./gradlew test
./gradlew lintDebug
./gradlew assembleDebug
./gradlew koverHtmlReportDebug
./gradlew koverXmlReportDebug
./gradlew koverVerifyDebug
./gradlew :shared:allTests
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
```

Notes:

- `ktlintCheck` verifies Kotlin formatting across `:app` and `:shared`. The
  formatter is **not** run automatically in CI; violations fail the build so
  they must be fixed locally (`./gradlew ktlintFormat`).
- `detekt` performs static analysis on Kotlin sources of both modules.
- `lintDebug` passes with warnings and no errors.
- Kover measures code coverage from the **JVM unit tests only**
  (`testDebugUnitTest`). It does not include Android instrumented tests or the
  shared iOS tests; the verification thresholds remain on the `:app` module.
- `:shared:allTests` runs the common tests on the JVM host and on the
  `iosSimulatorArm64` simulator. The framework link task verifies the iOS
  export (`SignalBriefShared.framework`) without an iOS app.
- Instrumented tests (`connectedDebugAndroidTest`) exist but are **not**
  executed in CI yet; they require a device or emulator.

## CI

The GitHub Actions workflow (`.github/workflows/android_ci.yml`) runs on pull
requests targeting `main`, on pushes to `main`, and on manual dispatch. It
validates the Gradle wrapper, then runs `ktlintCheck`, `detekt`, `test`,
`lintDebug`, `assembleDebug`, and the Kover report/verification tasks on
`ubuntu-latest` with JDK 17. No NewsAPI key is required in CI. The shared KMP
module is included in these gates (static analysis and its JVM host tests);
iOS-specific tasks (simulator tests, framework link) require Xcode and are
planned as a macOS CI job.

Workflow reports are uploaded as artifacts with a 7-day retention:

- unit-test reports (`app/build/reports/tests/`)
- Android Lint reports (`app/build/reports/lint-results-debug.*`)
- ktlint reports (`app/build/reports/ktlint/`)
- detekt reports (`app/build/reports/detekt/`)
- Kover reports (`app/build/reports/kover/`)

Dependency review (`actions/dependency-review-action@v5`) runs on pull requests
targeting `main` and fails on `moderate` severity vulnerabilities.

## Roadmap

See [IMPLEMENTATION_ROADMAP.md](IMPLEMENTATION_ROADMAP.md) for the phased plan:
verified Android baseline, extended quality gates, shared KMP domain module
(this milestone), shared data modules, offline-first storage with Room, shared
presentation, Compose Multiplatform UI, and the full product MVP. The roadmap
also separates features that belong in the public repository from commercial
capabilities (payments, production synchronization, analytics, and signing)
that are planned for a private repository.

## License

```
   Copyright (C) 2026 Konrad Szewczuk (@recipesforsoftware-pl)

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
```
