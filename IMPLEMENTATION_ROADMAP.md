# SignalBrief Implementation Roadmap

## 1. Executive summary

The current repository is an Android-only news reader (`:app`, namespace `com.recipesforsoftware.mvvm`) built with Jetpack Compose, Hilt, MVVM, Retrofit/Gson, Coroutines/StateFlow, DataStore, and Coil. It renders a single "Top Headlines" feed from NewsAPI in a Compose UI with light/dark theme support. The unit-test baseline (20 tests) and the debug build are green; Android Lint passes with warnings only. There is no CI, no offline storage, no navigation library, no search, no saved content, and no iOS target. The README overstates the implementation ("production-level", "clean architecture", "comprehensive testing").

SignalBrief is the product evolution of this codebase: a production-oriented Kotlin Multiplatform application for Android and iOS. A local design and product prototype in `/ui` (read-only, untracked) specifies the full target product: five-tab navigation (Brief, Feed, Monitor, Saved, Settings), onboarding, a Daily Brief reader, personalized feed, search, article details, story clusters, topic monitoring, collections, offline reading, guest/authenticated usage, and a free/Pro boundary.

This roadmap defines the smallest coherent MVP derived from the prototype, the target architecture, an incremental migration from the current Android app (not a destructive rewrite), a vertical-slice delivery order, testing and quality strategy, risks and decisions, and the exact first implementation PR.

## 2. Verified current state

Repository facts (verified on branch `docs/signalbrief-implementation-roadmap`, commit `a02cba5`, tag `android-baseline-v1`, matching `origin/main`):

- Single Gradle module `:app`. Root project name `News-MVVM-Compose`; app label "Recipes News". No `.github` workflows.
- Build: Gradle 9.5.0, AGP 9.3.1, Kotlin 2.3.21, KSP, Compose BOM 2026.04.01, Hilt 2.60.1, Retrofit 3.0.0 + Gson, Coil 3.0.4, DataStore Preferences 1.1.7. `compileSdk 37`, `targetSdk 35`, `minSdk 24`, JDK 17 bytecode.
- Architecture as implemented: one `TopHeadlineActivity`, one `TopHeadlineViewModel` (StateFlow + sealed `UiState.Loading/Success/Error`), `TopHeadlineRepository` wrapping Retrofit in `kotlin.Result`, Gson DTOs used directly as UI models, `ArticleCard` opening URLs via Custom Tabs, Material 3 theme with dynamic color and a DataStore-backed dark-mode toggle (`ThemePreference`, `ThemeViewModel`).
- Validation executed and passing:
  - `./gradlew tasks --all` — success.
  - `./gradlew test` — success; 20 unit tests, 0 failures (Repository 6, ViewModel 6, Theme 8).
  - `./gradlew lintDebug` — success; 26 warnings, 0 errors (mostly dependency-version updates, plus `UseKtx`, `UnusedResources`, `OldTargetApi`, `ObsoleteSdkInt`, `MonochromeLauncherIcon`, `NotShrinkingResources`).
  - `./gradlew assembleDebug` — success.
  - 20 instrumented test methods exist (Compose UI, DataStore, package) but were not run (no emulator); they require `connectedDebugAndroidTest`.
- Secrets: `NEWS_API_KEY` is read from untracked `local.properties` into `BuildConfig` (locally non-empty). No tracked file contains credentials, keys, signing material, or private endpoints. This is a client-embedded key — extractable, not secret.

Material discrepancies between README and code:

- README claims "clean architecture" and "production-level practices"; in reality the UI consumes Gson DTOs directly and there is no domain layer, repository contract, typed failure model, or offline source of truth.
- README claims "responsive UI"; the only adaptation is a standard Compose list and a dark-mode toggle.
- README clone URL points to `recipesforsoftware/news-mvvm-compose.git`; the real remote is `recipesforsoftware-pl/signalbrief.git`.
- README lists versions that drift from the catalog (e.g. Kotlin, Compose BOM, Hilt are accurate; minor claims about "comprehensive testing" overstate coverage).

## 3. Target MVP product

The target product is derived from `/ui` (every screen below is represented in `ui/shared/app.js`, the CSS tokens, or the HTML shells). Demo copy and prices in the prototype are placeholder content, not API contracts.

### MVP Core (public Android + iOS)
- Onboarding (4 steps: 3 pitch steps + interests/language/region/notification preference), guest by default.
- Five-tab shell: Brief, Feed, Monitor, Saved, Settings.
- Daily Brief with progress ring/bar and a full-screen story reader (save, share, open source, completion state).
- Personalized Feed with topic chips, featured story, refresh, and skeleton loading.
- Article details with source metadata, bookmark, mark-read, open-in-publisher (platform port).
- Saved articles with an All/Offline filter.
- Basic search over fetched and cached content, with an empty state.
- Offline cache: cached headlines and saved articles readable without network; offline banner and pills.
- Theme and basic settings (including language/region).
- Android and iOS.

### MVP Complete (public Android + iOS)
- Collections: create/rename/delete with article counts and offline availability.
- Topic monitoring: dashboard, activity bars, detail, pause/delete (free limit 3).
- Monitor creation flow (5-step wizard).
- Advanced offline management: storage view, auto-download briefing, Wi-Fi-only downloads, clear cache.
- Notification preferences.

The project must reach a stable Android/iOS MVP Core before implementing advanced Monitor and Collection workflows.

### Post-MVP public features
- Story clusters (timeline, source comparison) once the MVP is validated.
- Fully adaptive tablet/web layouts (navigation rail, split-pane list/detail).

### Private commercial features (later, private repository)
- Authenticated cross-device synchronization (Apple/Google/email).
- Production push notifications and reliable real-time/background alert delivery.
- Play Billing + StoreKit with the free/Pro boundary (3 vs unlimited monitors, 2 vs unlimited collections, immediate alerts, full offline, clusters).
- Production backend proxy, analytics, remote configuration, signing/deployment secrets.

## 4. Target architecture

High-level layout:

```text
androidApp     Hilt graph, platform adapters, Compose host
shared/        commonMain: domain, data, presentation, design system; androidMain; iosMain
iosApp         SwiftUI/Compose host + explicit composition root
```

Intended responsibilities:

- **Domain (commonMain, framework-free).** `Topic`, `Article`, `StoryCluster`, `Briefing`, `Monitor`, `Collection`, `Entitlement` value models with invariants; repository interfaces (`NewsRepository`, `SavedRepository`, `MonitorRepository`, `SettingsRepository`); use cases only where rules are non-trivial (brief generation, monitor matching, cluster grouping); typed failures (network, rate-limit, invalid-data, authorization, unknown).
- **Data.** Ktor Client + `kotlinx.serialization` in shared networking; Room 3 KMP as the offline-first source of truth; DTO/entity/domain/UI model separation with validation at the mapper boundary; explicit cache metadata and refresh policy; schema export and migration tests.
- **Presentation.** Shared ViewModels/state holders where justified, with immutable `UiState` and explicit `Action`/`Effect` contracts; the local database flow drives the UI; cached content stays visible during refresh and recoverable failures.
- **Platform ports.** Small interfaces (`ExternalNavigator`, `ShareService`, `NotificationScheduler`, `SecureStorage`, `ConnectivityObserver`, `PurchaseService`, `AppReviewService`) implemented in platform source sets; `expect/actual` only when the type is inherently platform-specific.
- **DI (decision).** Android application graph → Dagger/Hilt; `commonMain` → constructor injection, no DI framework; iOS application graph → explicit composition root. Hilt modules provide shared factories at the Android boundary; the iOS root constructs the same factories directly.

Module policy: start with `androidApp`, `shared`, `iosApp` plus a handful of shared packages. Extract `core:*` / `feature:*` modules only when boundaries are stable and a module demonstrably removes coupling (extraction criteria: distinct test scope, separate release cadence, or enforced dependency direction). Do not create one module per package or class.

## 5. Incremental migration strategy

Do not replace the Android app with a generated KMP project in one step. Deliver phased, reviewable PRs (provisional order, refine after each phase):

1. **Verified Android baseline and rebrand** — verified Android baseline; honest README with correct clone references; SignalBrief root project name and Android display name; documented development run mode (user-provided NewsAPI key in ignored `local.properties`) and planned production-connected mode (authorized provider, optionally via backend proxy); basic CI running unit tests, `lintDebug`, and `assembleDebug`. No KMP, no redesign.
2. **Extended Android quality gates** — ktlint; detekt; Kover; dependency review; improved test reporting; warning cleanup policy.
3. **Domain boundaries** — repository interface, DTO→domain mapping, typed failures, regression tests, while the existing Android UI keeps working.
4. **KMP shared module** — introduce `shared`; move pure domain and repository contracts to `commonMain`; Android Hilt graph still composes them.
5. **Shared networking** — Ktor Client + `kotlinx.serialization` in shared paths; platform engines (`androidMain`/`iosMain`); keep existing Android screens functional.
6. **Room KMP offline-first** — database as source of truth, migrations, refresh-then-cache flow.
7. **Shared presentation** — move eligible ViewModels and UI contracts to common code; preserve Hilt acquisition via factories/bindings; explicit iOS composition.
8. **Compose Multiplatform design system and navigation** — shared tokens/typography/components, type-safe shared navigation, platform adapters.
9. **Android/iOS vertical feature slices** — deliver features (section 6), maintaining both targets.
10. **Hardening** — adaptive layouts, accessibility, security, observability, release hardening, macOS CI.

Each phase must leave the repository buildable and each feature slice independently testable.

## 6. Vertical feature delivery

Deliver features as vertical slices after the foundations of section 5 are reliable. Slices 1–4 plus theme/basic settings and offline cache form MVP Core; collections, topic monitoring, and the monitor creation flow complete the MVP (section 3). Provisional order:

1. **Feed and article details** — value: core reading loop; shared: feed/brief domain, repository contract, UI models; Android: Hilt wiring, list + details; iOS: composition root, same UI; persistence: headline cache; tests: repository (MockEngine), ViewModel (Turbine), Compose UI, Kotest domain; acceptance: offline shows cached headlines, refresh preserves content.
2. **Saved articles** — value: bookmarks; shared: saved state, bookmark use case; persistence: Room tables; tests: DB + ViewModel; acceptance: bookmark toggles survive restart and offline.
3. **Daily Brief** — value: differentiated product; shared: brief generation, progress state; tests: BDD brief-generation scenarios; acceptance: progress persists, completion state renders.
4. **Search** — shared: query + result ranking; tests: ranking/filtering; acceptance: search works on cached and saved content.
5. **Collections** — shared: collection aggregate; persistence; acceptance: create/rename/delete with count and offline availability.
6. **Topic monitoring** — shared: monitor config, matching, activity model; free limit 3; acceptance: create/edit/pause/delete monitor, matches appear.
7. **Story clusters** — shared: grouping and neutral summary (Pro boundary); acceptance: cluster timeline + source comparison.
8. **Settings and offline management** — theme, text size, notifications, storage bar, clear cache.
9. **Authentication and synchronization boundary** — shared sync contract + platform auth ports; actual sync backend stays private.
10. **Pro conversion boundary** — shared `Entitlement` + `PurchaseService` contract; real billing SDKs stay private.

Payment SDKs, production synchronization, production analytics, and signing secrets remain outside the public repository.

## 7. Testing and quality strategy

- **Common tests:** Kotest `BehaviorSpec`/`FeatureSpec` Given/When/Then for domain rules; fakes for repository/clock/dispatcher seams.
- **JVM/Android tests:** MockK at Android/external boundaries; `kotlinx-coroutines-test` virtual time; Turbine for Flow assertions; Ktor MockEngine for network behavior; Room in-memory databases and migration tests; Hilt test components for integration.
- **UI tests:** Compose UI tests for critical user flows; screenshot tests for design-system regressions once stable in the toolchain; accessibility checks (content descriptions, touch targets, font scaling).
- **iOS:** shared native tests, simulator framework/app build, selected iOS tests via macOS CI (macOS runners only).
- **Static analysis and coverage:** ktlint (official style), detekt with type resolution, Android Lint, dependency/vulnerability scanning, Kover for JVM/common coverage (not claimed as native iOS coverage). No broad baselines; temporary baseline only for legacy findings with a reduction plan.
- **CI:** Linux workflow (wrapper validation, formatting, detekt, lint, shared/JVM tests, Android tests, debug assembly, coverage, dependency review) and macOS workflow (native tests, iOS build, selected iOS tests). Pin actions, set minimal permissions, keep secrets out of fork PRs.

## 8. Risks and decisions

- **Client-embedded API keys** are extractable from any APK/IPA. Mitigation: bring-your-own-key dev mode; production uses an authorized provider and, where necessary, a backend proxy (documented ADR).
- **NewsAPI licensing and limits** (no production/commercial use without an authorized tier; developer tier restrictions). Mitigation: keep provider behind the shared `NewsRepository` contract; support deterministic demo data; plan provider/proxy swap.
- **Monitor alerts in the public repository** — public monitors operate only on fetched and cached content; reliable real-time or background alerts require private backend and push-notification infrastructure. Mitigation: implement monitor matching over cached content in the public repo and defer production alert delivery to the private repository's push infrastructure.
- **Migration complexity** — risk of a long-running rewrite. Mitigation: incremental phases with reviewable PRs; every phase stays buildable and tested.
- **Hilt unavailable in `commonMain`** — accepted constraint. Mitigation: constructor injection in shared code, shared factories, Hilt only at the Android boundary.
- **Target role mentions Koin.** Decision: Hilt remains the Android DI; `commonMain` is framework-agnostic; iOS uses explicit composition. A short-lived `spike/koin-composition-root` branch + an ADR comparing Hilt vs Koin is acceptable. Never run two production DI containers for the same graph.
- **Room/KMP migration risk** — schema drift and migration bugs. Mitigation: schema export from the first production schema, migration tests, Room KMP vetted before adoption (ADR vs SQLDelight).
- **iOS build requires macOS** — cannot validate on Linux. Mitigation: macOS CI runners; shared code tests run on Linux; native paths only merged with macOS evidence.
- **Over-modularization** — premature module extraction adds friction. Mitigation: start minimal, define extraction criteria, prefer packages initially.
- **Over-building the prototype before validating the core feed.** Mitigation: features follow the vertical-slice order; validate Feed → Saved → Brief before monitors/clusters/paywall.

## 9. Recommended first implementation PR

Branch: `chore/verified-android-baseline`. Scope (small, reviewable):

- Preserve current Android behavior (no behavior changes).
- Rewrite the README so claims match the code; fix stale clone URL and repository references (`recipesforsoftware-pl/signalbrief`); document current architecture and verified commands.
- Rename the root project and Android display name to SignalBrief (root project name in `settings.gradle.kts`, `app_name` string). Do not change package/namespace/applicationId in this PR.
- Add a basic Android CI workflow running `./gradlew test`, `./gradlew lintDebug`, and `./gradlew assembleDebug` on the default branch (Linux). Instrumented tests exist but are not yet part of this workflow; managed-device or emulator execution is planned as a later quality-gate step.
- Document the currently available development mode: a user-provided NewsAPI key stored in ignored `local.properties`.
- Document the planned production-connected mode: an authorized provider and, where necessary, a backend proxy. State explicitly that the production-connected mode and deterministic demo fixtures are not implemented in this baseline PR; deterministic demo fixtures are planned as a later dedicated PR.

Acceptance criteria: CI green on the PR; README no longer claims unverified capabilities; app builds, unit tests pass, lint has no new errors; display name reads "SignalBrief".

Out of scope: KMP module, package/namespace/applicationId changes, UI redesign, dependency upgrades, adding ktlint/detekt wiring beyond the minimal CI, and any feature work.

## 10. Definition of done

- **Roadmap task:** README verified against code; baseline commands executed and results recorded; `/ui`, the skill directory, and `OPENCODE_SUMMARY.md` remain untracked; no implementation performed; no commit or push.
- **First implementation PR:** acceptance criteria above met; CI green; README accurate; diff clean of secrets; reviewable single-purpose change.
- **Public KMP MVP:** Android and iOS apps deliver the section-3 MVP; offline-first reading works; shared business logic and justified shared UI live in `commonMain`; Hilt composes the Android graph and the iOS root composes explicitly; unit, UI, DB, and migration tests pass in CI on both platforms; accessibility and adaptive layouts reviewed; no credentials or private endpoints in the repository.
