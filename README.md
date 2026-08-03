# SignalBrief

SignalBrief is a production-oriented Android news reader. It is being developed
incrementally: this repository currently contains a verified **Android-only**
baseline. The product direction — a Kotlin Multiplatform application for Android
and iOS with offline-first reading, saved articles, search, topic monitoring, and
a Daily Brief — is defined in the [implementation roadmap](IMPLEMENTATION_ROADMAP.md)
and is **future work**, not yet implemented.

## Current status

- **Platform:** Android only. There is no iOS target and no shared Kotlin
  Multiplatform module yet.
- **Scope:** a single-module Gradle application (`:app`) that renders a "Top
  Headlines" feed from NewsAPI in one Compose screen with light and dark theme
  support.
- **Not yet implemented:** Kotlin Multiplatform, Compose Multiplatform, iOS,
  offline-first storage, navigation, search, saved articles, topic monitoring,
  the Daily Brief, payments, synchronization, and a production backend. See the
  roadmap for the planned work.

## Implemented features

- Fetches and displays US top headlines from NewsAPI.
- Loading state, error state with retry, and success list rendering.
- Refresh action in the top app bar.
- Article cards open the original article in Chrome Custom Tabs.
- Material 3 theme with dynamic color support (Android 12+) and a persisted
  dark-mode toggle backed by DataStore.

## Architecture

MVVM with a single-activity Compose UI:

```
UI (Compose)  ->  TopHeadlineViewModel (StateFlow + sealed UiState)  ->  TopHeadlineRepository  ->  Retrofit (NewsAPI)
                                                                                                      |
                                                       ThemeViewModel <-> ThemePreference (DataStore)
```

- One launcher activity (`TopHeadlineActivity`).
- `TopHeadlineViewModel` exposes a `StateFlow<UiState>` with sealed
  `Loading` / `Success` / `Error` states.
- `TopHeadlineRepository` wraps Retrofit calls in `kotlin.Result`.
- Gson DTOs are used directly by the UI; there is no separate domain layer yet.
- Hilt provides the application graph and ViewModels.
- A `ThemePreference` (DataStore) plus `ThemeViewModel` persist and expose the
  dark-mode setting.

This is the current, honest state of the code. The target architecture (shared
domain, data, and presentation layers for Android and iOS) is described in the
[implementation roadmap](IMPLEMENTATION_ROADMAP.md).

## Technology stack

| Category | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose, Material 3 |
| Architecture | MVVM with StateFlow |
| Dependency injection | Dagger/Hilt |
| Networking | Retrofit + Gson |
| Image loading | Coil 3 |
| Persistence | DataStore Preferences |
| Async | Coroutines + Flow |
| Browser | Chrome Custom Tabs |
| Build | Gradle wrapper, AGP |
| Unit testing | JUnit 4, MockK, Turbine, kotlinx-coroutines-test, Robolectric |
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
    │   │   ├── model/                    # Gson DTOs
    │   │   └── repository/TopHeadlineRepository.kt
    │   ├── di/                           # Hilt network module and qualifiers
    │   ├── ui/
    │   │   ├── base/UiState.kt
    │   │   ├── components/ArticleCard.kt
    │   │   ├── screens/TopHeadlineScreen.kt
    │   │   ├── theme/                    # Color, Theme, Type, ThemePreference, ThemeViewModel
    │   │   └── topheadline/              # TopHeadlineActivity, TopHeadlineViewModel
    │   └── utils/AppConstant.kt
    ├── test/                             # JVM unit tests
    └── androidTest/                      # Instrumented (device) tests
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
# All JVM unit tests (fast, no device needed)
./gradlew test

# Instrumented tests (require a connected device or emulator)
./gradlew connectedDebugAndroidTest
```

- **Unit tests** cover the repository (network success/error mapping),
  the top-headlines ViewModel (loading/success/error flows), and the theme
  ViewModel (dark-mode persistence).
- **Instrumented tests** cover the top-headlines Compose screen, DataStore
  theme-preference behavior, and application package verification. They exist in
  `app/src/androidTest` but are **not** part of the CI workflow yet; executing
  them requires a device or emulator.

## Verified commands

The following commands pass on the current baseline:

```bash
./gradlew test
./gradlew lintDebug
./gradlew assembleDebug
```

Android Lint (`lintDebug`) passes with warnings and no errors.

## CI

A basic GitHub Actions workflow (`.github/workflows/android_ci.yml`) runs on pull
requests targeting `main`, on pushes to `main`, and on manual dispatch. It runs
`./gradlew test`, `./gradlew lintDebug`, and `./gradlew assembleDebug` on
`ubuntu-latest` with JDK 17. No NewsAPI key is required in CI.

## Roadmap

See [IMPLEMENTATION_ROADMAP.md](IMPLEMENTATION_ROADMAP.md) for the phased plan:
verified Android baseline (this PR), extended quality gates, shared KMP domain
and data modules, offline-first storage with Room, shared presentation, Compose
Multiplatform UI, and the full product MVP. The roadmap also separates features
that belong in the public repository from commercial capabilities (payments,
production synchronization, analytics, and signing) that are planned for a
private repository.

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
