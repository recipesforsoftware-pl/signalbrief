# SignalBrief

SignalBrief is a Kotlin Multiplatform news reader for **Android and iOS** with an
offline-first, typed-provenance feed. A shared KMP domain/data layer (`:shared`),
a shared Compose Multiplatform UI module (`:shared-ui`), and thin platform hosts
(`:app` with Hilt on Android, `iosApp` with a manual composition root on iOS)
deliver the same two-page onboarding flow and "Top Headlines" screen on both
platforms.

This is the current, honest state of the project — an open-source application
baseline, not a store publication and not a production backend. NewsAPI is used
**directly for local development only** with a developer-supplied key (see
[Local setup](#local-setup)).

## Screenshots

| | Onboarding | Top Headlines — light | Top Headlines — dark |
| --- | --- | --- | --- |
| Android | <img src="docs/screenshots/android/01_onboarding.png" alt="Android onboarding" width="160"/> | <img src="docs/screenshots/android/02_feed_light.png" alt="Android feed in light theme" width="160"/> | <img src="docs/screenshots/android/03_feed_dark.png" alt="Android feed in dark theme" width="160"/> |
| iOS | <img src="docs/screenshots/ios/01_onboarding.png" alt="iOS onboarding" width="160"/> | <img src="docs/screenshots/ios/02_feed_light.png" alt="iOS feed in light theme" width="160"/> | <img src="docs/screenshots/ios/03_feed_dark.png" alt="iOS feed in dark theme" width="160"/> |

## Implemented capabilities

- **Android and iOS apps** sharing one Kotlin Multiplatform domain/data layer
  and one Compose Multiplatform UI module.
- **Top Headlines feed** from NewsAPI (US by default), rendered in a single
  shared screen with loading (skeleton cards), empty, error-with-retry, and
  success states on both platforms.
- **Offline-first cache**: the latest successful remote response per country is
  stored in a shared Room KMP database and served as a fallback when the network
  fails. `TopHeadlinesFeed` is tagged `FeedSource.NETWORK` or
  `FeedSource.CACHE`, and a "Showing saved headlines" banner appears when the
  feed comes from the cache.
- **Two-page onboarding** with paging, "Skip"/"Start reading", and at-most-once
  host completion. Persisted with DataStore Preferences on Android and
  NSUserDefaults on iOS; returning users never see an onboarding flash.
- **Article images** loaded with Coil 3 through the Ktor 3 network fetcher.
- **Safe external article opening**: article URLs are validated as `http`/`https`
  before being handed to the platform browser (Chrome Custom Tabs on Android).
- **Light and dark themes** shared across platforms; the Android host keeps a
  persisted dark-mode toggle. Dynamic color is opt-in and off by default so the
  editorial palette stays consistent.
- **Responsive feed**: content is capped at a 600dp reading measure and centered
  on wide screens.
- **Typed failures** (`NewsFailure.Network` / `InvalidData` / `Unknown`);
  coroutine cancellation is always rethrown, never reported as a failure.
- **Branded identity**: launcher/AppIcon and splash/launch screens on both
  platforms; application identifier
  `pl.recipesforsoftware.signalbrief`.

## Architecture

MVVM with a clean domain boundary and shared presentation. Four Gradle/Xcode
units — the exact responsibility of each, the dependency direction, the
Hilt/iOS composition, and the data flow are documented in
[docs/ARCHITECTURE.md](docs/ARCHITECTURE.md):

```text
:app (Android host, Hilt)  ──┐
                             ├──> :shared-ui (shared Compose UI + presenter) ──> :shared (domain + data) ──> NewsAPI + Room
iosApp (iOS host, manual) ───┘
```

In short:

- **`:shared`** — Kotlin Multiplatform. Domain contracts and models, typed
  failures, Ktor networking (DTOs + mapping), Room KMP cache, and the
  network-first `OfflineFirstNewsRepository`. No UI, no DI framework.
- **`:shared-ui`** — Kotlin Multiplatform Compose UI: the `SignalBriefApp`
  shell, onboarding, the stateless `TopHeadlinesScreen`, a framework-independent
  `TopHeadlinesPresenter` (StateFlow), design tokens, and the shared theme.
- **`:app`** — Android application: `SignalBriefApplication` (`@HiltAndroidApp`),
  `MainActivity`, Hilt modules, Android-specific dark-mode and onboarding
  persistence.
- **`iosApp`** — iOS application: SwiftUI host embedding
  `SignalBriefSharedUi.framework`; `MainViewController.kt` builds the shared
  graph manually and reads the key from the app bundle.

## Offline-first data flow

```text
UI observes StateFlow from the shared presenter
  -> refresh requests the remote source
  -> DTOs are validated and mapped to domain models
  -> the country/feed cache is replaced transactionally in Room
  -> fresh data is returned tagged FeedSource.NETWORK
  on NewsFailure.Network with a non-empty cache
  -> cached data is returned tagged FeedSource.CACHE (banner shown)
```

A failed remote request never touches the cache; `InvalidData`/`Unknown`
failures are never hidden by cached content.

## Technology stack

| Category | Technology |
|---|---|
| Language | Kotlin, Kotlin Multiplatform, Swift (iOS host) |
| UI | Compose Multiplatform, Material 3 (shared screen on both platforms) |
| Architecture | MVVM, unidirectional StateFlow + shared presenter |
| DI | Dagger/Hilt (Android only); manual composition root (iOS) |
| Networking | Ktor 3 client + kotlinx.serialization (Android and iOS engines) |
| Image loading | Coil 3 (Ktor 3 network fetcher) |
| Persistence | Room (KMP), DataStore Preferences (Android), NSUserDefaults (iOS) |
| Async | Coroutines + Flow |
| Browser | Chrome Custom Tabs (Android) |
| Testing | JUnit 4, MockK, Turbine, kotlinx-coroutines-test, Robolectric, kotlin.test, Compose UI test, Espresso |
| Build | Gradle wrapper, AGP, Xcode |

## Local setup

NewsAPI is used **for local development only**. No real key is stored in this
repository; you supply your own key in an ignored, local-only file per platform.
A client-side key is extractable from the built APK/IPA, so this setup must not
be treated as a production credential.

1. Clone the repository:
   ```bash
   git clone https://github.com/recipesforsoftware-pl/signalbrief.git
   ```
2. Register for a developer key at [NewsAPI.org](https://newsapi.org/register).
3. Store the key locally (see the platform sections below). Never commit
   `local.properties` or `Secrets.xcconfig`; both are already in `.gitignore`.

Without a valid key the apps build and run, but headline requests fail.

### Android key

Create `local.properties` at the repository root:

```properties
NEWS_API_KEY=your_news_api_key
```

The value is embedded into `BuildConfig.NEWS_API_KEY` at build time and sent in
the `X-Api-Key` header.

### iOS key

Copy the tracked template and fill in your key:

```bash
cp iosApp/Configuration/Secrets.example.xcconfig iosApp/Configuration/Secrets.xcconfig
```

```xcconfig
NEWS_API_KEY=your_news_api_key
```

The value is injected into `Info.plist` via the xcconfig and read by
`MainViewController.kt`. Only `Secrets.example.xcconfig` (with a placeholder) is
tracked.

## Running the app — Android

```bash
# Debug build
./gradlew assembleDebug

# Install on a connected device or emulator
./gradlew installDebug

# Launch
adb shell am start -n pl.recipesforsoftware.signalbrief/.ui.main.MainActivity
```

## Running the app — iOS

```bash
open iosApp/iosApp.xcodeproj   # then Run (⌘R)
```

or from the command line:

```bash
xcodebuild -project iosApp/iosApp.xcodeproj -scheme iosApp -configuration Debug \
  -sdk iphonesimulator -destination 'platform=iOS Simulator,name=iPhone 17 Pro' \
  -derivedDataPath iosApp/build CODE_SIGNING_ALLOWED=NO build
```

## Tests and quality gates

```bash
# Shared modules: JVM host tests + iOS simulator tests + framework links
./gradlew :shared:allTests :shared-ui:allTests
./gradlew :shared:linkDebugFrameworkIosSimulatorArm64 :shared-ui:linkDebugFrameworkIosSimulatorArm64

# Android JVM unit tests, lint, formatting, static analysis, debug build
./gradlew test lintDebug ktlintCheck detekt assembleDebug

# Coverage report + verification
./gradlew :app:koverHtmlReportAll :app:koverXmlReportAll :app:koverVerifyAll

# Instrumented tests (require a device or emulator)
./gradlew connectedDebugAndroidTest
```

Notes:

- `ktlintCheck` enforces Kotlin formatting across `:app`, `:shared`, and
  `:shared-ui`; fix locally with `./gradlew ktlintFormat`.
- `detekt` runs static analysis on all modules.
- Kover measures coverage from the **JVM unit tests only** and aggregates
  `:shared`, `:shared-ui`, and `:app` into a custom `all` variant. **Kover
  verification passes with the existing 9% line-coverage threshold**.
- Instrumented tests exist in `app/src/androidTest` but are **not** run in CI
  yet; they require a device or emulator.

## CI

Three GitHub Actions workflows protect `main`:

- **Android CI** (`android_ci.yml`, Ubuntu): Gradle wrapper validation,
  `ktlintCheck`, `detekt`, `test`, `lintDebug`, `assembleDebug`, and Kover
  report/verification. No NewsAPI key required.
- **KMP and iOS CI** (`kmp_ios_ci.yml`, macOS): shared tests, framework links,
  `ktlintCheck`, `detekt`, and an unsigned iOS simulator build of the host app.
  It creates a temporary git-ignored `Secrets.xcconfig` with an obvious fake key
  for the build only.
- **Dependency review** (`dependency_review.yml`): fails on moderate-or-higher
  vulnerabilities in pull requests.

## Current limitations

- Navigation framework, search, saved articles, topic monitoring, the Daily
  Brief, payments, synchronization, and a production backend are **not
  implemented**.
- Direct NewsAPI use with a client-side key is **local development only**; it is
  not a production-safe secret architecture. A future production-connected mode
  would use an authorized provider and, where required, a backend proxy.
- There are no deterministic demo fixtures yet.
- Instrumented tests are not part of CI.

## Roadmap

The phased plan — from the verified Android baseline through the KMP foundation,
offline-first storage, and the full product MVP — is in
[IMPLEMENTATION_ROADMAP.md](IMPLEMENTATION_ROADMAP.md). Commercial capabilities
(payments, production synchronization, analytics, signing) are deliberately
scoped to a separate private repository.
