# SignalBrief

SignalBrief is a production-oriented news reader for Android and iOS that is
being migrated incrementally to Kotlin Multiplatform. This repository currently
contains a verified **Android app and an iOS app that share both a Kotlin
Multiplatform domain/network module** (`:shared`) **and a Compose Multiplatform
UI module** (`:shared-ui`) that renders the same "Top Headlines" screen on both
platforms. The product direction — offline-first reading, saved articles,
search, topic monitoring, and a Daily Brief — is defined in the
[implementation roadmap](IMPLEMENTATION_ROADMAP.md) and is **future work**.

## Current status

- **Platforms:** Android app (`:app`), iOS app (`iosApp`), and two Kotlin
  Multiplatform shared modules: `:shared` (domain boundary + Ktor network data
  layer) and `:shared-ui` (framework-independent presenter + Compose
  Multiplatform UI, targets Android and iOS).
- **Scope:** a "Top Headlines" feed from NewsAPI rendered in one shared Compose
  screen with light and dark theme support. `:app` keeps only the Hilt
  composition root and the Android dark-mode menu; the rest of the screen
  (presenter, strings, theme, composables) is shared with iOS.
- **Not yet implemented:** offline-first storage, navigation, search, saved
  articles, topic monitoring, the Daily Brief, payments, synchronization, and a
  production backend. See the roadmap for the planned work.

## Implemented features

- Fetches and displays US top headlines from NewsAPI on **Android and iOS**.
- Shared loading state, empty state, error state with retry, and success list
  rendering (`:shared-ui`).
- Refresh action in the shared top app bar.
- Material 3 theme shared between platforms; Android additionally has dynamic
  color support (Android 12+) and a persisted dark-mode toggle backed by
  DataStore rendered through the shared screen's `topBarActions` slot.
- iOS app (`iosApp`) embeds the shared `SignalBriefSharedUi.framework` and reads
  its NewsAPI key from a git-ignored `Secrets.xcconfig`.

## Architecture

MVVM with a single-activity Compose UI, a clean domain boundary, and shared
presentation. The domain boundary, the network data layer, and the screen itself
are shared Kotlin Multiplatform code reused by both apps:

```
Android UI (:app) ──┐
                    ├──> TopHeadlinesScreen (shared UI, :shared-ui) ──> TopHeadlinesPresenter (StateFlow, :shared-ui)
iOS UI (iosApp) ────┘                                                                          |
                                                                                               v
                                                                        NewsRepository (domain contract, :shared)
                                                                                               |
                                                                                               v
                                                                          KtorNewsRepository (:shared) -> Ktor + kotlinx.serialization (NewsAPI)
                                                                                               |
                                                                  ArticleDto --ArticleMapper--> Article (domain model, :shared)
```

- The shared `TopHeadlinesScreen` (Compose Multiplatform, `:shared-ui`) is a
  stateless composable that receives `TopHeadlinesUiState` (sealed
  `Loading` / `Success` / `Empty` / `Error`) and callbacks from the host. A
  `topBarActions` slot lets Android inject its dark-mode menu while iOS renders
  the same core screen.
- `TopHeadlinesPresenter` (`:shared-ui`) is framework-independent: it owns its
  `CoroutineScope`, exposes a `StateFlow<TopHeadlinesUiState>`, guards against
  stale responses with a request-generation counter, and must be disposed by the
  host. Both `TopHeadlineViewModel` (Android, Hilt) and `MainViewController`
  (iOS) delegate to it.
- `NewsRepository` (domain contract), `Article`/`Source` (domain models), and
  `NewsFailure` (typed failures) live in `:shared:commonMain`. They are
  framework-independent: no Ktor, serialization, Android, Hilt, or transport DTO
  imports.
- `KtorNewsRepository` (data layer, `:shared:commonMain`) is the only code
  touching Ktor and kotlinx.serialization. It maps `ArticleDto` to the domain
  `Article` and translates transport exceptions into typed `NewsFailure` values
  (`Network`, `InvalidData`, `Unknown`). Cancellation is always rethrown.
- The HTTP client is created by an `expect`/`actual` factory: the Android engine
  is wired on Android, the Darwin engine on iOS, and `MockEngine` in common
  tests. Client configuration (content negotiation, timeouts, response
  validation, base URL, API-key header) is shared and identical on both
  platforms.
- Hilt (Android composition root only) provides `NewsApiConfig` from
  `BuildConfig.NEWS_API_KEY`, builds the shared `HttpClient`, and binds
  `NewsRepository` to `KtorNewsRepository` in `RepositoryModule`.
- On iOS the same `KtorNewsRepository` is created in `MainViewController.kt`,
  which reads `NEWS_API_KEY` from the app's `Info.plist` (injected from the
  git-ignored `Secrets.xcconfig`).
- A `ThemePreference` (DataStore) plus `ThemeViewModel` persist and expose the
  dark-mode setting on Android.

This is the current, honest state of the code. The target architecture (shared
presentation and UI for Android and iOS) is described in the
[implementation roadmap](IMPLEMENTATION_ROADMAP.md).

## Technology stack

| Category | Technology |
|---|---|
| Language | Kotlin, Kotlin Multiplatform, Swift (iOS host) |
| UI | Compose Multiplatform, Material 3 (shared screen on Android and iOS) |
| Architecture | MVVM with StateFlow + shared presenter |
| Dependency injection | Dagger/Hilt (Android only) |
| Networking | Ktor 3 client + kotlinx.serialization (Android + iOS engines) |
| Image loading | Coil 3 |
| Persistence | DataStore Preferences |
| Async | Coroutines + Flow |
| Browser | Chrome Custom Tabs (Android article opening) |
| Build | Gradle wrapper, AGP, Xcode (iosApp) |
| Unit testing | JUnit 4, MockK, Turbine, kotlinx-coroutines-test, Robolectric, kotlin.test |
| UI testing | Compose UI test, Espresso |

## Project structure

```
app/
├── build.gradle.kts
└── src/
    ├── main/java/com/recipesforsoftware/mvvm/
    │   ├── NewsApplication.kt            # @HiltAndroidApp
    │   ├── di/                           # Hilt composition root (news config + repository binding)
    │   ├── ui/
    │   │   ├── theme/                    # Color, Theme, Type, ThemePreference, ThemeViewModel
    │   │   └── topheadline/              # TopHeadlineActivity, TopHeadlineViewModel, DarkModeMenu
    │   └── utils/AppConstant.kt
    ├── test/                             # JVM unit tests
    └── androidTest/                      # Instrumented (device) tests
shared/
├── build.gradle.kts                      # KMP: android + iosArm64 + iosSimulatorArm64 + iosX64
└── src/
    ├── commonMain/kotlin/com/recipesforsoftware/mvvm/
    │   ├── data/
    │   │   ├── NewsApiConfig.kt          # Base URL, API-key header, timeout config
    │   │   ├── HttpClientFactory.kt      # expect factory; content negotiation, timeouts, validation
    │   │   ├── repository/KtorNewsRepository.kt  # NewsRepository implementation (Ktor)
    │   │   ├── remote/dto/               # kotlinx.serialization DTOs (ArticleDto, SourceDto, TopHeadlinesResponseDto)
    │   │   └── remote/mapper/            # DTO -> domain mapping (ArticleMapper)
    │   └── domain/
    │       ├── failure/NewsFailure.kt    # Typed failures: Network, InvalidData, Unknown
    │       ├── model/                    # Domain models (Article, Source)
    │       └── repository/NewsRepository.kt  # Domain contract
    ├── androidMain/kotlin/.../data/      # HttpClientFactory.android.kt (OkHttp engine)
    ├── iosMain/kotlin/.../data/          # HttpClientFactory.ios.kt (Darwin engine)
    └── commonTest/kotlin/com/recipesforsoftware/mvvm/
        ├── data/                         # Common tests: KtorNewsRepository (MockEngine), ArticleMapper
        └── domain/                       # Common tests (models, failures, repository contract)
shared-ui/
├── build.gradle.kts                      # KMP: android + iosArm64 + iosSimulatorArm64; framework SignalBriefSharedUi
└── src/
    ├── commonMain/kotlin/com/recipesforsoftware/mvvm/ui/topheadlines/
    │   ├── TopHeadlinesPresenter.kt      # Framework-independent StateFlow presenter (dispose() contract)
    │   ├── TopHeadlinesUiState.kt        # Sealed Loading / Success / Empty / Error
    │   ├── TopHeadlinesError.kt          # Typed errors + Throwable mapping (CancellationException rethrown)
    │   ├── TopHeadlinesStrings.kt        # Centralized user-facing strings + error bodies
    │   ├── SignalBriefTheme.kt           # Shared light/dark Material 3 theme
    │   ├── TopHeadlinesScreen.kt         # Shared stateless screen (topBarActions slot)
    │   └── components/ArticleCard.kt     # Text-first shared article card
    ├── commonTest/.../TopHeadlinesPresenterTest.kt   # 11 presenter tests (JVM + iOS simulator)
    └── iosMain/kotlin/.../MainViewController.kt      # ComposeUIViewController + iOS repository wiring
iosApp/
├── iosApp.xcodeproj/                     # Xcode project; "Compile Kotlin Framework" = embedAndSignAppleFrameworkForXcode
├── iosApp/                               # SwiftUI host: iosApp.swift, ContentView.swift, Info.plist, Assets.xcassets
└── Configuration/
    ├── Secrets.example.xcconfig          # Tracked template with placeholder
    └── Secrets.xcconfig                  # Git-ignored; real NEWS_API_KEY for the iOS app
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

The current development mode reads a user-supplied NewsAPI key from **ignored,
local-only files** — one per platform:

- Android: `local.properties`

```properties
NEWS_API_KEY=your_news_api_key
```

- iOS: `iosApp/Configuration/Secrets.xcconfig` (copy the tracked
  `Secrets.example.xcconfig` template)

```xcconfig
NEWS_API_KEY=your_news_api_key
```

Notes:

- `local.properties` and `Secrets.xcconfig` must **never be committed**. Both are
  listed in `.gitignore`; only `Secrets.example.xcconfig` (with a placeholder) is
  tracked.
- CI never uses a real key. The macOS workflow creates a temporary
  `Secrets.xcconfig` with an obvious fake value for the unsigned Xcode build and
  never uploads it; the Android workflow needs no key at all.
- On Android the key is embedded into `BuildConfig` at build time and sent in the
  `X-Api-Key` header. On iOS it is injected into `Info.plist` via the xcconfig
  and read by `MainViewController.kt`. A client-side key is extractable from the
  APK/IPA, so this setup is intended for **local development only**, never as a
  secure production credential.
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
# Android: debug build
./gradlew assembleDebug

# Android: install on a connected device/emulator
./gradlew installDebug

# iOS: build and launch on a simulator via Xcode
open iosApp/iosApp.xcodeproj   # then Run (⌘R)
# or from the command line (after opening the workspace once):
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug \
  -sdk iphonesimulator -destination 'platform=iOS Simulator,name=iPhone 17 Pro' \
  -derivedDataPath iosApp/build CODE_SIGNING_ALLOWED=NO build
```

Without a valid key in `local.properties` (Android) or `Secrets.xcconfig` (iOS),
the app builds and runs but headline requests fail — the development key is
required to see live data.

## Testing

```bash
# Android app JVM unit tests
./gradlew test

# Shared KMP module: JVM host tests + iOS simulator tests + iOS framework link
./gradlew :shared:allTests
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64

# Shared UI module: JVM host tests + iOS simulator tests + iOS framework link
./gradlew :shared-ui:allTests
./gradlew :shared-ui:linkDebugFrameworkIosSimulatorArm64

# Instrumented tests (require a connected device or emulator)
./gradlew connectedDebugAndroidTest
```

- **Android unit tests** cover the top-headlines ViewModel
  (loading/success/typed-error flows) and the theme ViewModel (dark-mode
  persistence). Data-layer behavior is covered by the shared common tests
  instead.
- **Shared common tests** cover the domain models (`Article` value semantics),
  the typed failures (`NewsFailure`), the DTO-to-domain mapper (happy path and
  missing/invalid remote data), and the `NewsRepository` implementation against
  Ktor's `MockEngine` (success, typed failure mapping, cancellation
  propagation, and the platform-specific success/failure paths). They run both
  on the JVM (`:shared:testAndroidHostTest`) and on the iOS simulator
  (`:shared:iosSimulatorArm64Test`).
- **Shared UI tests** (`:shared-ui`) cover the presenter end to end with a fake
  repository: initial loading, success/empty, every typed error, retry,
  cancellation on dispose, and stale-response protection. They run on the JVM
  host and on the iOS simulator (`:shared-ui:allTests`).
- **Instrumented tests** cover the shared Top Headlines Compose screen
  (including the Android dark-mode menu), DataStore theme-preference behavior,
  and application package verification. They exist in `app/src/androidTest` but
  are **not** part of the CI workflow yet; executing them requires a device or
  emulator.

## Quality gates

The following commands pass on the current state:

```bash
./gradlew ktlintCheck
./gradlew detekt
./gradlew test
./gradlew lintDebug
./gradlew assembleDebug
./gradlew :app:koverHtmlReportAll
./gradlew :app:koverXmlReportAll
./gradlew :app:koverVerifyAll
./gradlew :shared:allTests
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64
./gradlew :shared-ui:allTests
./gradlew :shared-ui:linkDebugFrameworkIosSimulatorArm64
```

Notes:

- `ktlintCheck` verifies Kotlin formatting across `:app`, `:shared`, and
  `:shared-ui`. The formatter is **not** run automatically in CI; violations
  fail the build so they must be fixed locally (`./gradlew ktlintFormat`).
- `detekt` performs static analysis on Kotlin sources of all modules.
- `lintDebug` passes with warnings and no errors.
- Kover measures code coverage from the **JVM unit tests only**
  (`testDebugUnitTest`). It does not include Android instrumented tests or the
  shared iOS tests. Coverage is aggregated into a custom Kover variant named
  `all` (`:app` debug variant plus the `:shared` and `:shared-ui` Android/KMP
  projects); the 9% LINE threshold is verified against that aggregated report
  via `:app:koverVerifyAll`.
- `:shared:allTests` and `:shared-ui:allTests` run the common tests on the JVM
  host and on the `iosSimulatorArm64` simulator. The framework link tasks verify
  the iOS exports (`SignalBriefShared.framework`,
  `SignalBriefSharedUi.framework`).
- Instrumented tests (`connectedDebugAndroidTest`) exist but are **not**
  executed in CI yet; they require a device or emulator.
- The iOS app (`iosApp`) builds locally with Xcode and has been verified on the
  iPhone simulator; it is built by the macOS "KMP and iOS CI" workflow described
  below.

## CI

Two GitHub Actions workflows plus dependency review protect the `main` branch.

### Android CI (`android_ci.yml`)

Runs on **Ubuntu** (`ubuntu-latest`, JDK 17) on pull requests targeting `main`,
on pushes to `main`, and on manual dispatch. It validates the Gradle wrapper,
then runs `ktlintCheck`, `detekt`, `test`, `lintDebug`, `assembleDebug`, and the
Kover report/verification tasks (using the aggregated `all` variant:
`:app:koverHtmlReportAll`, `:app:koverXmlReportAll`, `:app:koverVerifyAll`). The
shared KMP modules are included in these gates (static analysis, JVM host tests,
and Kover coverage aggregation for both `:shared` and `:shared-ui`); the
iOS-specific tasks (simulator tests, framework link) require Xcode and are
handled by the macOS workflow below. No NewsAPI key is required.

### KMP and iOS CI (`kmp_ios_ci.yml`)

Runs on **macOS** (pinned `macos-26`, Apple Silicon, JDK 21) on the same
triggers, filtered to changes in Gradle configuration, the version catalog and
wrapper, `shared/`, `shared-ui/`, `iosApp/`, and workflow files. It validates
the Gradle wrapper and runs:

```bash
./gradlew :shared:allTests
./gradlew :shared-ui:allTests
./gradlew :shared-ui:linkDebugFrameworkIosSimulatorArm64
./gradlew ktlintCheck
./gradlew detekt
```

then performs an unsigned iOS simulator build of the host app with the Gradle
framework embed step enabled:

```bash
xcodebuild \
  -project iosApp/iosApp.xcodeproj \
  -scheme iosApp \
  -configuration Debug \
  -sdk iphonesimulator \
  -destination 'generic/platform=iOS Simulator' \
  -derivedDataPath iosApp/build \
  CODE_SIGNING_ALLOWED=NO \
  ARCHS=arm64 \
  build
```

Notes:

- `ARCHS=arm64` pins the simulator build to the arm64 slice because `:shared-ui`
  declares `iosSimulatorArm64`/`iosArm64` targets (no `iosX64`); the generic
  simulator destination would otherwise request both simulator architectures and
  fail.
- **No real API key and no signing credentials are used.** The workflow creates
  a temporary, git-ignored `iosApp/Configuration/Secrets.xcconfig` containing an
  obvious fake value (`CI_FAKE_NEWS_API_KEY_DO_NOT_USE`) before the Xcode build.
  The key is only read at app runtime, never during the build or tests, so no
  live NewsAPI request is made.
- The simulator **runtime is not launched** in CI; the unsigned simulator build
  is the verified slice.
- Reports (shared/shared-ui test reports, ktlint/detekt reports, and a focused
  Xcode build log) are uploaded as artifacts with 7-day retention, only on
  failure. No build trees, frameworks, `.kexe`, `.dSYM`, DerivedData, or secret
  configuration are uploaded.
- Gradle and Xcode checks each run in a single `macos-26` job with `concurrency`
  canceling obsolete runs for the same branch/PR and `permissions:
  contents: read`.

The exact local equivalents are the commands listed above (Gradle tasks in the
"Quality gates" section and the `xcodebuild` invocation in "Running the app").

### Workflow artifacts (Android CI)

Uploaded with 7-day retention:

- unit-test reports (`app/build/reports/tests/`)
- Android Lint reports (`app/build/reports/lint-results-debug.*`)
- ktlint reports (`app/build/reports/ktlint/`)
- detekt reports (`app/build/reports/detekt/`)
- Kover reports (`app/build/reports/kover/`)

### Dependency review

`actions/dependency-review-action@v5` runs on pull requests targeting `main`
and fails on `moderate` severity vulnerabilities.

## Roadmap

See [IMPLEMENTATION_ROADMAP.md](IMPLEMENTATION_ROADMAP.md) for the phased plan:
verified Android baseline, extended quality gates, shared KMP domain module,
shared data modules, shared presentation, Compose Multiplatform UI (first shared
screen, this milestone), offline-first storage with Room, and the full product
MVP. The roadmap also separates features that belong in the public repository
from commercial capabilities (payments, production synchronization, analytics,
and signing) that are planned for a private repository.

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
