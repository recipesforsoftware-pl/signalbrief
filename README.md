# SignalBrief

SignalBrief is a production-oriented news reader for Android and iOS that is
being migrated incrementally to Kotlin Multiplatform. This repository currently
contains a verified **Android app and an iOS app that share both a Kotlin
Multiplatform domain/network module** (`:shared`) **and a Compose Multiplatform
UI module** (`:shared-ui`) that renders the two-page onboarding flow and the
"Top Headlines" screen through a shared app shell on both platforms. The product
direction — offline-first reading, saved articles, search, topic monitoring, and
a Daily Brief — is defined in the
[implementation roadmap](IMPLEMENTATION_ROADMAP.md) and is **future work**.

## Current status

- **Platforms:** Android app (`:app`), iOS app (`iosApp`), and two Kotlin
  Multiplatform shared modules: `:shared` (domain boundary + Ktor network data
  layer) and `:shared-ui` (framework-independent presenter + Compose
  Multiplatform UI, targets Android and iOS).
- **Scope:** a shared `SignalBriefApp` shell that shows a two-page onboarding
  flow on first launch and then the "Top Headlines" feed from NewsAPI rendered
  in one shared Compose screen with light and dark theme support. `:app` keeps
  only the Hilt composition root and the Android dark-mode menu; the rest of the
  screen (presenter, strings, theme, composables) is shared with iOS.
- **Not yet implemented:** navigation framework, search, saved articles, topic
  monitoring, the Daily Brief, payments, synchronization, and a production
  backend. See the roadmap for the planned work.

## Implemented features

- Fetches and displays US top headlines from NewsAPI on **Android and iOS**.
- Offline-first top headlines: the latest successful remote response per country
  is cached in a shared Room database (KMP, `:shared`) and served when the
  network fails, on both Android and iOS.
- Typed feed provenance: the repository returns a `TopHeadlinesFeed` tagged with
  `FeedSource.NETWORK` or `FeedSource.CACHE`, so the UI knows whether the
  articles are fresh network content or a persistent-cache fallback without
  peeking into the data layer.
- Two-page shared onboarding flow (`:shared-ui`): an editorial visual, page
  indicator, "Continue"/"Back" paging, and "Skip"/"Start reading" completion.
  It is shown on first launch and skipped on subsequent launches once completed.
- Shared `SignalBriefApp` shell (`:shared-ui`) that decides between the
  onboarding flow and the Top Headlines screen based on the persisted onboarding
  flag, with a brief loading state while the flag is being read so returning
  users never see an onboarding flash.
- Onboarding completion persistence: **DataStore Preferences on Android**
  (`OnboardingPreference` + `OnboardingViewModel`, Hilt-provided) and
  **NSUserDefaults on iOS** (read synchronously before Compose starts in
  `MainViewController`).
- Shared design tokens (`:shared-ui`): editorial color palette, typography
  scale, shapes, and spacing, plus the `SignalBriefPrimaryButton` and
  `OnboardingPageIndicator` components.
- Light and dark theme support shared between platforms. The Android host keeps
  a persisted dark-mode toggle; dynamic color is opt-in and **disabled by
  default** so the editorial palette stays consistent on both platforms.
- Shared loading state, empty state, error state with retry, and success list
  rendering (`:shared-ui`). Loading is shown as animated skeleton cards, and the
  empty/error states offer a retry action.
- Saved-headlines banner: when the feed is served from the persistent cache
  (`FeedSource.CACHE`), the shared screen shows an "Showing saved headlines"
  banner above the list so users are never silently reading stale content.
- Redesigned shared article card: a source badge with source initials, a
  two-line headline, a three-line excerpt, and a fixed-size thumbnail loaded
  with Coil 3 through the Ktor 3 network fetcher (no OkHttp). The whole card is
  clickable and opens the article URL in the platform browser via the shared
  `LocalUriHandler` host wiring; an AutoMirrored open-arrow indicator shows the
  action.
- Responsive top headlines list: content is capped at a 600dp reading measure
  and centered on wide screens (e.g. tablets).
- Refresh action in the shared top app bar.
- iOS app (`iosApp`) embeds the shared `SignalBriefSharedUi.framework` and reads
  its NewsAPI key from a git-ignored `Secrets.xcconfig`.
- Branded app icons and splash/launch screens on both platforms: Android uses
  the Android 12+ platform splash (backported via
  `androidx.core:core-splashscreen`) with adaptive and monochrome launcher
  icons; iOS uses `Assets.xcassets` app icon and a `UILaunchScreen` launch
  image on the SignalBrief blue background.

## Screenshots

Captured on an Android emulator (1080×2340) and an iPhone 17 Pro simulator
(1206×2622) with a valid NewsAPI key. The files in `docs/screenshots/` are
downscaled to a display-friendly resolution for this document.

| | Onboarding | Top Headlines — light | Top Headlines — dark |
| --- | --- | --- | --- |
| Android | <img src="docs/screenshots/android/01_onboarding.png" alt="Android onboarding" width="160"/> | <img src="docs/screenshots/android/02_feed_light.png" alt="Android feed in light theme" width="160"/> | <img src="docs/screenshots/android/03_feed_dark.png" alt="Android feed in dark theme" width="160"/> |
| iOS | <img src="docs/screenshots/ios/01_onboarding.png" alt="iOS onboarding" width="160"/> | <img src="docs/screenshots/ios/02_feed_light.png" alt="iOS feed in light theme" width="160"/> | <img src="docs/screenshots/ios/03_feed_dark.png" alt="iOS feed in dark theme" width="160"/> |

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
                                                              OfflineFirstNewsRepository (:shared)  // network-first, cache fallback
                                                                               |                                  |
                                                                               v                                  v
                                                              NewsRemoteDataSource (:shared)     NewsLocalDataSource (:shared)
                                                                               |                                  |
                                                                               v                                  v
                                                        KtorNewsRemoteDataSource (:shared)     RoomNewsLocalDataSource (:shared, Room KMP)
                                                                               |                                  |
                                                                               v                                  v
                                                       Ktor + kotlinx.serialization (NewsAPI)     SignalBriefDatabase (CachedArticleDao)
```

- The shared `SignalBriefApp` shell (`:shared-ui`) owns the top-level decision
  between onboarding and the main screen. It keeps `TopHeadlinesScreen`
  stateless, avoids a navigation framework for the two destinations, and lets
  each host supply the onboarding flag plus its own Top Headlines content. The
  two-page paging state is owned by a small `OnboardingPresenter`
  (`:shared-ui`, `StateFlow`-based); its current page index is persisted with
  `rememberSaveable` (via a small `Saver`), so page 1 stays on page 1 across
  recompositions and the flow resumes on the saved page (for example page 2)
  after host recreation such as an Android configuration change. Both "Skip" and
  "Start reading" are routed through a per-shell `OnboardingCompletion` guard so
  the host completion callback fires at most once per shell instance.
- The shared `TopHeadlinesScreen` (Compose Multiplatform, `:shared-ui`) is a
  stateless composable that receives `TopHeadlinesUiState` (sealed
  `Loading` / `Success` / `Empty` / `Error`) and callbacks from the host. Its
  `Success` state carries the typed feed provenance (`FeedSource`), and the
  screen renders an animated skeleton list while loading, a saved-headlines
  banner above the list when the feed comes from the cache, and centered,
  capped-width content (600dp reading measure) on wide screens. A
  `topBarActions` slot lets Android inject its dark-mode menu while iOS renders
  the same core screen.
- Onboarding completion is persisted per platform: Android uses a dedicated
  DataStore Preferences file (`OnboardingPreference` via Hilt) exposed through
  `OnboardingViewModel`; iOS reads and writes NSUserDefaults synchronously in
  `MainViewController`. In both cases the host flips the shell flag
  optimistically on completion so the switch is immediate.
- `TopHeadlinesPresenter` (`:shared-ui`) is framework-independent: it owns its
  `CoroutineScope`, exposes a `StateFlow<TopHeadlinesUiState>`, guards against
  stale responses with a request-generation counter, and must be disposed by the
  host. Both `TopHeadlineViewModel` (Android, Hilt) and `MainViewController`
  (iOS) delegate to it.
- `NewsRepository` (domain contract), `Article`/`Source`/`TopHeadlinesFeed`/
  `FeedSource` (domain models), and `NewsFailure` (typed failures) live in
  `:shared:commonMain`. They are framework-independent: no Ktor, serialization,
  Android, Hilt, or transport DTO imports. `getTopHeadlines` returns
  `Result<TopHeadlinesFeed>` so callers see both the articles and whether they
  came from the network or the cache.
- The data layer (`:shared:commonMain`) splits network and storage behind two
  interfaces. `KtorNewsRemoteDataSource` (the only code touching Ktor and
  kotlinx.serialization) maps `ArticleDto` to the domain `Article` and
  translates transport exceptions into typed `NewsFailure` values (`Network`,
  `InvalidData`, `Unknown`). `RoomNewsLocalDataSource` persists the cache
  through `SignalBriefDatabase` (`CachedArticleDao` +
  `CachedArticleEntity`) and translates database failures into
  `NewsFailure.Unknown`. Cancellation is always rethrown in both.
- `OfflineFirstNewsRepository` implements `NewsRepository` with a network-first
  policy: on remote success it deduplicates articles by URL (first occurrence
  wins, original order preserved), returns a `FeedSource.NETWORK` feed, and
  transactionally replaces that country's cache with the unique list (remote
  `emptyList` clears it);
  on `NewsFailure.Network` it falls back to the non-empty cache and returns a
  `FeedSource.CACHE` feed, keeping the original network failure when the cache
  is empty; invalid-data, unknown, and cancellation failures are never hidden by
  the cache, and a failed remote request never mutates the cache. Country caches
  are isolated (one `country` + `top-headlines` feed key pair each).
- The HTTP client is created by an `expect`/`actual` factory: the Android engine
  is wired on Android, the Darwin engine on iOS, and `MockEngine` in common
  tests. Client configuration (content negotiation, timeouts, response
  validation, base URL, API-key header) is shared and identical on both
  platforms.
- Hilt (Android composition root only) provides `NewsApiConfig` from
  `BuildConfig.NEWS_API_KEY`, builds the shared `HttpClient` and the
  `SignalBriefDatabase` singleton, and binds `NewsRepository` to
  `OfflineFirstNewsRepository` in `RepositoryModule`/`DatabaseModule`.
- On iOS the same offline-first chain is created in `MainViewController.kt`,
  which reads `NEWS_API_KEY` from the app's `Info.plist` (injected from the
  git-ignored `Secrets.xcconfig`). The iOS composition owns the presenter, the
  HTTP client, and the database, and disposes all three together when the
  controller disappears.
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
| Image loading | Coil 3 (Ktor 3 network fetcher, no OkHttp) |
| Persistence | Room (KMP), DataStore Preferences (Android), NSUserDefaults (iOS onboarding) |
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
    │   │   ├── onboarding/               # OnboardingPreference (DataStore), OnboardingViewModel (Hilt)
    │   │   └── topheadline/              # TopHeadlineActivity, TopHeadlineViewModel, DarkModeMenu
    │   └── utils/AppConstant.kt
    ├── test/                             # JVM unit tests
    └── androidTest/                      # Instrumented (device) tests
shared/
├── build.gradle.kts                      # KMP: android + iosArm64 + iosSimulatorArm64 + iosX64
└── src/
    ├── commonMain/kotlin/com/recipesforsoftware/mvvm/
    │   ├── data/
    │   │   ├── remote/
    │   │   │   ├── NewsApiConfig.kt, HttpClientFactory.kt, NewsRemoteDataSource.kt, KtorNewsRemoteDataSource.kt
    │   │   │   ├── dto/                   # kotlinx.serialization DTOs (ArticleDto, SourceDto, TopHeadlinesResponseDto)
    │   │   │   └── mapper/                # DTO -> domain mapping (ArticleMapper)
    │   │   ├── local/
    │   │   │   ├── NewsLocalDataSource.kt, RoomNewsLocalDataSource.kt, DatabaseConstants.kt
    │   │   │   ├── dao/                   # CachedArticleDao (replaceAll is transactional)
    │   │   │   ├── db/                    # SignalBriefDatabase, SignalBriefDatabaseConstructor
    │   │   │   ├── entity/                # CachedArticleEntity (composite key country/feed/url)
    │   │   │   └── mapper/                # domain <-> entity mapping (CachedArticleMapper)
    │   │   └── repository/OfflineFirstNewsRepository.kt  # NewsRepository implementation (network-first + cache fallback, typed provenance)
    │   └── domain/
    │       ├── failure/NewsFailure.kt    # Typed failures: Network, InvalidData, Unknown
    │       ├── model/                    # Domain models (Article, Source, TopHeadlinesFeed, FeedSource)
    │       └── repository/NewsRepository.kt  # Domain contract
    ├── androidMain/kotlin/.../data/      # HttpClientFactory.android.kt (OkHttp engine), SignalBriefDatabase androidActual
    ├── iosMain/kotlin/.../data/          # HttpClientFactory.ios.kt (Darwin engine), SignalBriefDatabase iosActual
    └── commonTest/kotlin/com/recipesforsoftware/mvvm/
        ├── data/                         # Common tests: remote (MockEngine), local (Room test DB), repository (offline-first policy)
        └── domain/                       # Common tests (models, failures, repository contract)
shared-ui/
├── build.gradle.kts                      # KMP: android + iosArm64 + iosSimulatorArm64; framework SignalBriefSharedUi
└── src/
    ├── commonMain/kotlin/com/recipesforsoftware/mvvm/ui/
    │   ├── app/SignalBriefApp.kt         # Shared shell: onboarding vs. Top Headlines, loading gate
    │   ├── designsystem/                 # Tokens (colors, typography, shapes, spacing) + shared components
    │   ├── onboarding/                   # Two-page onboarding: state, presenter, screen, page, visual, strings
    │   └── topheadlines/
    │       ├── TopHeadlinesPresenter.kt      # Framework-independent StateFlow presenter (dispose() contract)
    │       ├── TopHeadlinesUiState.kt        # Sealed Loading / Success / Empty / Error
    │       ├── TopHeadlinesError.kt          # Typed errors + Throwable mapping (CancellationException rethrown)
    │       ├── TopHeadlinesStrings.kt        # Centralized user-facing strings + error bodies
    │       ├── SignalBriefTheme.kt           # Shared light/dark Material 3 theme (design tokens)
    │       ├── TopHeadlinesScreen.kt         # Shared stateless screen (topBarActions slot)
    │       ├── images/SignalBriefImageLoader.kt  # Coil singleton + shared Ktor fetcher
    │       └── components/
    │           ├── ArticleCard.kt            # Image + source badge + headline card (Coil thumbnail)
    │           ├── ArticleCardFormatting.kt  # Pure formatting helpers (sourceInitials)
    │           ├── CacheNoticeBanner.kt      # "Showing saved headlines" banner (CACHE provenance)
    │           └── SkeletonArticleCard.kt    # Animated loading placeholder
    ├── commonTest/.../ui/
    │   ├── onboarding/OnboardingPresenterTest.kt    # Page-state tests (6)
    │   ├── onboarding/OnboardingCompletionTest.kt   # At-most-once host completion (4)
    │   ├── onboarding/OnboardingSaverTest.kt        # rememberSaveable save/restore round trip (3)
    │   ├── topheadlines/TopHeadlinesPresenterTest.kt  # 12 presenter tests (JVM + iOS simulator)
    │   └── topheadlines/components/ArticleCardFormattingTest.kt  # Pure formatting helpers (7)
    └── iosMain/kotlin/.../ui/topheadlines/MainViewController.kt  # ComposeUIViewController + iOS wiring + NSUserDefaults onboarding
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
  (loading/success/typed-error flows), the theme ViewModel (dark-mode
  persistence), and onboarding: the DataStore-backed `OnboardingPreference`
  (default value, persistence, cross-instance reads, `IOException` read fallback
  to not-completed, non-IO exceptions propagated, cancellation not swallowed)
  and `OnboardingViewModel` (null-until-loaded, state reflection, completion
  persistence). Data-layer behavior is covered by the shared common tests
  instead.
- **Shared common tests** cover the domain models (`Article` value semantics),
  the typed failures (`NewsFailure`), the DTO-to-domain mapper (happy path and
  missing/invalid remote data), the `KtorNewsRemoteDataSource` against Ktor's
  `MockEngine` (success, typed failure mapping, cancellation propagation), the
  `RoomNewsLocalDataSource` against a real Room test database (round-trip in
  stable order, replacement, empty-list clearing, country isolation, typed
  failures after close), and the `OfflineFirstNewsRepository` offline-first
  policy (cache write-through, replacement, empty-cache clearing, stable first-
  occurrence URL deduplication for both remote and cached feeds, fallback to a
  non-empty cache on network failure, never hiding non-network failures,
  cancellation propagation, no cache corruption on failure, country isolation,
  typed local failures, and typed feed provenance: fresh remote results are
  tagged `FeedSource.NETWORK` — including empty remote responses — while a
  network failure falls back to `FeedSource.CACHE`). They run both on the JVM
  (`:shared:testAndroidHostTest`) and on the iOS simulator
  (`:shared:iosSimulatorArm64Test`).
- **Shared UI tests** (`:shared-ui`) cover the onboarding page state (initial
  page, paging boundaries, restoring a presenter on page 2, the `rememberSaveable`
  save/restore round trip) and the app-shell completion flow (Skip and Start
  reading invoke the host completion exactly once; repeated taps cannot fire it
  more than once), plus the top headlines presenter end to end with a fake
  repository (initial loading, success/empty, every typed error, retry,
  cancellation on dispose, stale-response protection, and cache-provenance
  exposure). Pure formatting helpers (`ArticleCardFormatting`) and the article
  URL validation helper (`hasActionableUrl`) are covered by dedicated common tests.
  They run on the JVM host and on the iOS simulator (`:shared-ui:allTests`).
- **Instrumented tests** cover the shared Top Headlines Compose screen
  (including the Android dark-mode menu, article-card click callback wiring, and
  the refresh/retry actions), DataStore theme-preference behavior, and application
  package verification. They exist in `app/src/androidTest` but are **not** part of
  the CI workflow yet; executing them requires a device or emulator.

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
  projects). **Kover verification passes with the existing 9% line-coverage
  threshold**, checked against that aggregated report via
  `:app:koverVerifyAll`.
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
