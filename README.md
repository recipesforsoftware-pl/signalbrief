# News MVVM - Jetpack Compose Architecture Sample

A modern Android application built with **Jetpack Compose**, **Hilt**, **MVVM architecture**, and **Material Design 3**. Fetches top news headlines from the NewsAPI and presents them in a beautiful, responsive Compose UI.

This project demonstrates production-level Android development practices including clean architecture, dependency injection, reactive UI, proper error handling, and comprehensive testing.

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                      UI Layer                       │
│  ┌─────────────┐  ┌──────────────┐  ┌───────────┐  │
│  │   Compose   │  │   ViewModel  │  │  UiState  │  │
│  │   Screens   │◄─┤   (Hilt)    │◄─┤  (sealed) │  │
│  └─────────────┘  └──────┬───────┘  └───────────┘  │
├───────────────────────────┼─────────────────────────┤
│                    Data Layer                        │
│  ┌──────────────┐  ┌──────┴───────┐  ┌───────────┐  │
│  │   Repository │──┤    Network   │──┤  Retrofit  │  │
│  │   (Hilt)    │  │   Service    │  │  + Gson    │  │
│  └──────────────┘  └──────────────┘  └───────────┘  │
├─────────────────────────────────────────────────────┤
│                Dependency Injection                  │
│  ┌──────────────────────────────────────────────┐   │
│  │              Hilt (Dagger)                   │   │
│  │   @HiltAndroidApp · @AndroidEntryPoint       │   │
│  │   @HiltViewModel · @Inject · @Singleton      │   │
│  └──────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────┘
```

### Design Decisions

| Pattern | Implementation | Rationale |
|---------|---------------|-----------|
| **Architecture** | MVVM (Model-View-ViewModel) | Clean separation of concerns; ViewModel survives configuration changes |
| **UI** | Jetpack Compose + Material 3 | Declarative UI, less boilerplate, modern Android standard |
| **DI** | Hilt (Dagger) | Compile-time DI, Android lifecycle-aware, industry standard |
| **Networking** | Retrofit + Gson | Type-safe HTTP client with automatic JSON serialization |
| **Image Loading** | Coil 3 (Compose) | Kotlin-first, coroutine-native, Compose-optimized |
| **State Management** | StateFlow + sealed UiState | Type-safe state representation, coroutine-friendly |
| **Error Handling** | Kotlin Result | Idiomatic error propagation without exceptions leaking to UI |

## Tech Stack

| Category | Library | Version |
|----------|---------|---------|
| Language | Kotlin | 2.3.21 |
| UI | Jetpack Compose (BOM) | 2026.04.01 |
| Material | Material 3 | via Compose BOM |
| DI | Hilt (Dagger) | 2.60.1 |
| Networking | Retrofit | 3.0.0 |
| Serialization | Gson | via Retrofit |
| Image Loading | Coil 3 | 3.0.4 |
| Async | Coroutines + Flow | via Lifecycle |
| Browser | Chrome Custom Tabs | 1.8.0 |
| Testing | JUnit 4, MockK, Turbine | Various |

## Project Structure

```
app/src/main/java/com/recipesforsoftware/mvvm/
├── NewsApplication.kt              # @HiltAndroidApp Application class
├── data/
│   ├── api/
│   │   └── NetworkService.kt       # Retrofit API interface
│   ├── model/
│   │   ├── Article.kt              # Article data class (nullable fields)
│   │   ├── Source.kt               # Source data class
│   │   └── TopHeadlinesResponse.kt # API response wrapper
│   └── repository/
│       └── TopHeadlineRepository.kt # Data access with Result wrapping
├── di/
│   ├── NetworkModule.kt            # Hilt module for network dependencies
│   └── qualifiers.kt               # @BaseUrl qualifier
├── ui/
│   ├── base/
│   │   └── UiState.kt             # Sealed interface for UI states
│   ├── components/
│   │   └── ArticleCard.kt          # Reusable article card composable
│   ├── screens/
│   │   └── TopHeadlineScreen.kt    # Main screen composable
│   ├── theme/
│   │   ├── Color.kt                # Material 3 color tokens
│   │   ├── Theme.kt                # Dynamic color + dark theme
│   │   └── Type.kt                 # Typography scale
│   └── topheadline/
│       ├── TopHeadlineActivity.kt   # @AndroidEntryPoint Activity
│       └── TopHeadlineViewModel.kt  # @HiltViewModel with StateFlow
└── utils/
    └── AppConstant.kt              # Constants
```

## Key Features

- **Single-Activity Architecture**
- **Material Design 3** with Dynamic Color support (Android 12+)
- **Dark Theme** with automatic system preference detection
- **Edge-to-Edge** layout with immersive status bar
- **Pull-to-Refresh** pattern via top app bar action
- **Custom Chrome Tabs** for in-app article reading
- **Responsive Layout** adapting to different screen sizes
- **Smooth Image Loading** with Coil 3 and crossfade animations

## Testing

The project includes a comprehensive test suite:

### Unit Tests
- **TopHeadlineViewModelTest** - ViewModel state management, loading/success/error flows
- **TopHeadlineRepositoryTest** - Repository network calls, error handling, data mapping

### Instrumented Tests
- **AppInstrumentedTest** - App context and package verification

### Test Stack
| Library | Purpose |
|---------|---------|
| JUnit 4 | Test framework |
| MockK | Kotlin-native mocking |
| Turbine | Flow testing utilities |
| kotlinx-coroutines-test | Coroutine testing support |
| Robolectric | Android framework simulation |

### Running Tests

```bash
# Unit tests (fast, no device needed)
./gradlew :app:testDebugUnitTest

# Single test class
./gradlew :app:testDebugUnitTest --tests "com.recipesforsoftware.mvvm.TopHeadlineViewModelTest"

# All tests
./gradlew test
```

## Build Configuration

### Build Variants
The project supports debug and release build types:

```bash
# Debug build
./gradlew assembleDebug

# Release build (minified with ProGuard)
./gradlew assembleRelease

# Install debug on connected device
./gradlew installDebug
```

### ProGuard / R8
Release builds include code shrinking and obfuscation:
- Retrofit service interfaces preserved
- Gson model classes preserved
- Hilt generated code protected
- Line numbers kept for crash reports

## Requirements

- **Minimum SDK**: 24 (Android 7.0)
- **Target SDK**: 35 (Android 15)
- **Compile SDK**: 37
- **JDK**: 17

## Setup

1. Clone the repository:
   ```bash
    git clone https://github.com/recipesforsoftware/news-mvvm-compose.git
   ```

2. Open in Android Studio Ladybug or later.

3. Get an API key from [NewsAPI.org](https://newsapi.org/register).

4. Add your API key to `local.properties` (already in `.gitignore`):
   ```
   NEWS_API_KEY=your_api_key_here
   ```

5. Build and run:
   ```bash
   ./gradlew installDebug
   ```

## Dependencies

| Dependency | License |
|------------|---------|
| Jetpack Compose | Apache 2.0 |
| Hilt (Dagger) | Apache 2.0 |
| Retrofit | Apache 2.0 |
| Coil 3 | Apache 2.0 |
| Material 3 | Apache 2.0 |

### License
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

