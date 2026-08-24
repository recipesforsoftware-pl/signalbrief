# SignalBrief Implementation Roadmap

_Last updated: 2026-08-24_

## 1. Executive summary

SignalBrief is evolving incrementally from a verified Android-only news reader into a production-oriented Kotlin Multiplatform application for Android and iOS.

The repository already has a preserved Android baseline, an accurate public README, SignalBrief branding, and Android CI with formatting verification, static analysis, dependency review, JVM coverage reporting, Gradle Wrapper validation, and temporary report artifacts.

The next priority is not the complete product redesign. It is a small but real end-to-end Kotlin Multiplatform foundation that proves:

- shared code in `commonMain`;
- Android and iOS targets;
- shared domain and networking;
- a deliberate platform-boundary strategy;
- tests in `commonTest`;
- one working Compose Multiplatform screen on Android and iOS;
- Android and iOS CI;
- clear architecture decisions.

After that foundation is stable, the project continues toward the full SignalBrief MVP: offline-first reading, saved articles, Daily Brief, search, collections, monitoring, adaptive UI, and the later private commercial layer.

The migration must remain incremental. Every pull request must keep the repository buildable, reviewable, testable, and free of production credentials.

## 2. Verified current state

### Completed

- Repository renamed and publicly presented as **SignalBrief**.
- Root Gradle project and Android display name changed to SignalBrief.
- Android-only behavior preserved.
- README corrected to reflect the actual implementation.
- Public implementation roadmap added.
- Basic Android GitHub Actions workflow added.
- Verified baseline:
  - 20 JVM unit tests;
  - 20 Android instrumented test methods present;
  - instrumented tests are not yet executed in CI;
  - `test`, `lintDebug`, and `assembleDebug` pass.
- `android-baseline-v1` tag preserved before the migration work.
- Dagger/Hilt remains the Android dependency-injection choice.
- `commonMain` is required to remain independent of any DI framework.
- iOS dependencies will be assembled through an explicit composition root.

### Completed quality-gates phase

Branch:

```text
ci/android-quality-gates
```

Planned outcome:

- ktlint formatting verification;
- detekt static analysis;
- Kover JVM coverage reporting and verification;
- Gradle Wrapper validation;
- dependency review for pull requests;
- temporary CI report artifacts;
- accurate quality-gate documentation.

### Current implementation limitations

> This subsection records the state of the pre-migration Android baseline at the
> time this roadmap was written. The repository has since progressed through the
> KMP phases below (shared domain and network layer, Compose Multiplatform UI,
> offline-first Room cache, Android and iOS CI); see the README and
> `docs/ARCHITECTURE.md` for the current status. What remains **not implemented**
> today: topic monitoring, the Daily Brief reader, payments, synchronization,
> and a production backend.
> Saved articles persistence foundation (entity, DAO, repository, migration) is
> implemented; feed bookmark toggle is implemented; the Saved screen and minimal
> two-destination navigation (Headlines / Saved) are implemented; Article Details
> and local search over cached headlines are implemented. The Daily Brief
> foundation is implemented; its reader remains the next increment.

Current delivery status:

- Local Search: implemented
- Daily Brief foundation: implemented
- Daily Brief reader: next
- Remote Search: future/optional

The original Android-only baseline was:

- Android-only;
- one Gradle application module;
- based on Retrofit and Gson;
- directly exposing API DTOs to the UI;
- without a domain layer;
- without typed failures;
- without local database storage;
- without navigation;
- without search, saved articles, Daily Brief, monitoring, or iOS.

## 3. Delivery priorities

The roadmap is split into four delivery levels.

### Priority A — KMP Foundation Slice

This is the immediate technical milestone.

It proves the complete Android-to-iOS path with the smallest useful vertical slice. It deliberately comes before Room KMP, the full redesign, and advanced product features.

Required outcome:

- `shared` Kotlin Multiplatform module;
- `commonMain`, `commonTest`, `androidMain`, and `iosMain`;
- shared domain model;
- repository contract;
- DTO-to-domain mapping;
- typed failures;
- Ktor Client;
- `kotlinx.serialization`;
- Android and iOS Ktor engines;
- a justified use of `expect/actual` for an inherently platform-specific dependency;
- Android Hilt composition;
- framework-agnostic constructor injection in shared code;
- explicit iOS composition root;
- one shared Compose Multiplatform screen;
- successful Android build;
- successful iOS simulator build;
- shared tests;
- Linux and macOS CI;
- concise ADRs and architecture documentation.

### Priority B — Public MVP Core

The first coherent product release for Android and iOS:

- onboarding;
- Brief shell;
- personalized Feed;
- article details;
- saved articles;
- basic search;
- offline cache;
- theme and essential settings;
- deterministic demo data or documented bring-your-own-key development mode.

### Priority C — Public MVP Complete

After MVP Core is stable:

- collections;
- topic-monitor dashboard;
- monitor creation and configuration;
- advanced offline management;
- notification preferences;
- adaptive phone and tablet layouts;
- accessibility review;
- complete light and dark themes based on the SignalBrief design system.

### Priority D — Private commercial layer

A separate private repository may later contain:

- authenticated cross-device synchronization;
- production backend and provider proxy;
- Play Billing and StoreKit;
- subscription entitlement verification;
- production analytics;
- remote configuration;
- push infrastructure;
- signing and deployment secrets.

## 4. Target architecture

Initial target layout:

```text
androidApp/
shared/
  src/commonMain/
  src/commonTest/
  src/androidMain/
  src/iosMain/
iosApp/
```

Do not introduce the final large module graph immediately. Begin with the smallest stable structure and extract `core:*` or `feature:*` modules only when a dependency boundary is proven.

### Domain

`commonMain` contains framework-independent models and contracts:

- `Article`;
- `Topic`;
- `Briefing`;
- `Monitor`;
- `Collection`;
- repository interfaces;
- typed failure hierarchy;
- non-trivial use cases only.

Domain code must not depend on:

- Android SDK;
- UIKit;
- Room entities;
- Ktor response types;
- DI annotations;
- Compose UI.

### Data

The KMP foundation uses:

- Ktor Client;
- `kotlinx.serialization`;
- separate remote DTO and domain models;
- explicit mapping and validation;
- platform engines supplied from `androidMain` and `iosMain`.

Room KMP is introduced after the first Android/iOS networking slice is proven. The local database then becomes the source of truth.

### Presentation

Use MVVM with explicit unidirectional data flow:

```text
immutable UiState
user Action
optional transient Effect
```

Shared ViewModels or state holders are introduced only where they reduce real duplication and remain ergonomic for both platforms.

The first shared UI target is one complete feed-oriented screen, not the entire design prototype.

### Dependency injection

Accepted decision:

```text
Android app graph -> Dagger/Hilt
commonMain -> constructor injection, no DI framework
iOS app graph -> explicit composition root
```

Do not add Koin to the main production graph merely to list another technology.

A later isolated comparison spike may evaluate Koin, but it must not create two competing production containers.

### Platform boundaries

Prefer interfaces when a capability can be modeled as a port.

Use `expect/actual` only when the type or creation logic is inherently platform-specific, such as the Ktor engine or platform identity.

Document the choice in an ADR.

## 5. Accelerated incremental migration

### Phase 0 — Verified Android baseline

Status: **complete**

Delivered:

- baseline tag;
- SignalBrief rebrand;
- accurate README;
- basic Android CI;
- unchanged Android behavior.

### Phase 1 — Android quality gates

Status: **complete**

Branch:

```text
ci/android-quality-gates
```

Deliver:

- ktlint;
- detekt;
- Kover;
- wrapper validation;
- dependency review;
- report artifacts;
- documented JVM coverage limitations.

Exit criteria:

- all configured local tasks pass;
- remote CI is green;
- no broad analysis baseline;
- no behavior changes;
- squash merge into `main`.

### Phase 2 — Domain boundaries before KMP

Suggested branch:

```text
refactor/domain-boundaries
```

Deliver:

- domain `Article`;
- remote DTO separated from the domain model;
- DTO-to-domain mapper;
- `NewsRepository` interface;
- Android repository implementation;
- typed failures;
- cancellation-safe exception handling;
- regression tests;
- existing Android UI still works.

Exit criteria:

- UI no longer consumes Gson DTOs directly;
- repository contract has no Retrofit or Android types;
- failure states are typed;
- existing Android behavior remains unchanged;
- tests cover mapping, failures, and repository behavior.

### Phase 3 — KMP shared foundation

Suggested branch:

```text
feat/kmp-shared-foundation
```

Deliver:

- `shared` KMP module;
- Android and iOS targets;
- `commonMain`, `commonTest`, `androidMain`, `iosMain`;
- domain models, repository contract, typed failures moved to common code;
- Android Hilt continues composing shared dependencies;
- initial iOS composition root;
- common tests run successfully.

Exit criteria:

- Android app still builds and runs;
- iOS framework is generated;
- no Android dependency leaks into `commonMain`;
- shared code is covered by deterministic tests.

### Phase 4 — Shared networking

Suggested branch:

```text
feat/shared-networking-ktor
```

Deliver:

- Ktor Client;
- `kotlinx.serialization`;
- shared DTOs and mapping;
- Android engine;
- iOS engine;
- one justified `expect/actual` or platform factory boundary;
- Ktor `MockEngine` tests;
- Retrofit/Gson removed only after equivalent behavior is verified.

Exit criteria:

- Android feed uses shared networking;
- iOS can execute the same repository path;
- no real API key is committed;
- development provider rules remain documented.

### Phase 5 — First Android/iOS Compose slice

Suggested branch:

```text
feat/compose-multiplatform-feed-slice
```

Deliver:

- iOS application host;
- one shared Compose Multiplatform feed screen;
- shared screen contract and state;
- Android and iOS platform adapters for external article navigation;
- loading, success, empty, and recoverable error states;
- Android and iOS screenshots.

This phase does not implement the entire `/ui` redesign.

Exit criteria:

- the same feed screen renders on Android and iOS;
- article opening is delegated to platform adapters;
- Android Hilt and iOS composition root both assemble the slice;
- platform builds are reproducible.

### Phase 6 — Dual-platform CI and architecture evidence

Suggested branch:

```text
ci/kmp-android-ios
```

Deliver:

- Linux shared/JVM and Android validation;
- macOS shared native tests;
- iOS simulator build;
- selected iOS tests;
- `ARCHITECTURE.md`;
- `TESTING.md`;
- ADRs:
  - Hilt Android and framework-agnostic shared code;
  - interface versus `expect/actual`;
  - Ktor versus Retrofit;
  - shared Compose screen versus native SwiftUI;
- concise README update with verified commands.

Exit criteria:

- green Android and iOS CI;
- repository claims match executed checks;
- diagrams and ADRs explain trade-offs rather than list frameworks.

### Phase 7 — Room KMP and offline-first source of truth

Deliver:

- Room KMP;
- schema export;
- migration tests;
- article cache;
- refresh policy;
- database `Flow` as the UI source of truth;
- cached content preserved during recoverable refresh failures.

### Phase 8 — Shared presentation and navigation

Deliver:

- eligible shared ViewModels/state holders;
- type-safe navigation;
- design-system tokens;
- reusable Compose Multiplatform components;
- platform adapters for share, browser, notifications, and storage.

### Phase 9 — Public product slices

Deliver in this order:

1. Feed and article details;
2. saved articles;
3. Daily Brief;
4. search;
5. collections;
6. topic monitoring;
7. settings and offline management;
8. story clusters;
9. authentication boundary;
10. Pro entitlement boundary.

Do not begin advanced monitoring or commercial features before Feed, Saved, Brief, and offline behavior are stable on both platforms.

## 6. KMP Foundation Slice acceptance criteria

The immediate milestone is complete only when all of the following are true:

### Structure

- `shared` module exists;
- Android and iOS targets compile;
- source sets are correctly separated;
- `commonMain` has no Android SDK, UIKit, Hilt, Dagger, or Koin dependency.

### Shared code

- domain models are shared;
- repository contract is shared;
- typed failures are shared;
- networking is shared through Ktor and serialization;
- mapping is deterministic and tested.

### Platform code

- Android uses Hilt;
- iOS uses an explicit composition root;
- platform engine creation is isolated;
- external navigation is behind a platform port.

### UI

- one complete screen is shared with Compose Multiplatform;
- loading, content, empty, and recoverable error behavior is visible;
- Android and iOS screenshots are available.

### Testing and CI

- `commonTest` passes;
- Android unit tests pass;
- Android build passes;
- iOS simulator build passes;
- macOS CI validates the iOS path;
- no unexecuted platform is described as verified.

### Documentation

- README describes implemented KMP capability accurately;
- architecture diagram is current;
- material decisions have ADRs;
- no production credentials are present;
- AI-assisted work is presented as a reviewed engineering workflow, not autonomous code generation.

## 7. Testing and quality strategy

### Common tests

Use:

- Kotest BDD-style tests where compatible and valuable;
- `kotlinx-coroutines-test`;
- Turbine;
- fakes for repository and clock boundaries;
- Ktor `MockEngine`.

### Android

Use:

- JVM tests;
- MockK at external or Android boundaries;
- Hilt integration tests;
- Compose UI tests;
- Android Lint;
- ktlint;
- detekt;
- Kover for JVM coverage visibility.

### iOS

Use:

- native shared tests;
- simulator build;
- selected integration tests;
- macOS CI.

Kover results must never be presented as Android instrumented coverage or native iOS coverage.

### Pull-request discipline

Every PR must:

- have one coherent goal;
- preserve unrelated local work;
- include regression tests where applicable;
- pass `git diff --check`;
- avoid secrets and generated build outputs;
- be squash-merged so `main` keeps one logical commit per PR.

## 8. Documentation and technical presentation

After the KMP Foundation Slice reaches `main`, update:

- README;
- repository description and topics;
- architecture diagram;
- test matrix;
- ADR index;
- screenshots for Android and iOS;
- a short technical walkthrough.

The walkthrough should demonstrate:

- why the migration was incremental;
- what moved to `commonMain`;
- one justified platform boundary;
- Hilt on Android and explicit iOS composition;
- tests and CI;
- one AI-agent suggestion that was reviewed, changed, or rejected;
- how correctness was verified after generated changes.

Do not describe roadmap items as implemented until the corresponding merge and CI evidence exist.

## 9. Risks and mitigations

### Time pressure can encourage a rewrite

Mitigation:

- preserve the Android baseline;
- use small pull requests;
- migrate one working slice;
- postpone Room and the full redesign until Android/iOS sharing is proven.

### Client API keys are extractable

Mitigation:

- keep bring-your-own-key development mode;
- never treat a client key as secret;
- use an authorized production provider and backend proxy later.

### News provider licensing can block production use

Mitigation:

- keep provider logic behind `NewsRepository`;
- add deterministic demo fixtures later;
- document provider limitations.

### Hilt is unavailable in common code

Mitigation:

- constructor injection in `commonMain`;
- Hilt only at the Android boundary;
- explicit iOS composition;
- ADR documenting the decision.

### Koin may appear in role requirements

Mitigation:

- do not distort the main architecture;
- explain the trade-off;
- create an optional short-lived Koin composition spike only after the primary KMP slice is complete.

### iOS validation can be skipped accidentally

Mitigation:

- macOS CI is mandatory before claiming iOS support;
- require simulator build evidence for every platform-sensitive PR.

### Shared UI can become an all-or-nothing migration

Mitigation:

- begin with one screen;
- retain platform hosts and adapters;
- evaluate each later screen independently.

### Large formatting changes can hide semantic changes

Mitigation:

- isolate quality-gate formatting from architecture migration;
- inspect whitespace-insensitive diffs;
- avoid drive-by refactoring in tooling PRs.

## 10. Definition of done

### Android quality-gates phase

- ktlint, detekt, Kover, wrapper validation, dependency review, and report artifacts work locally and in CI;
- documented limitations are accurate;
- no intentional feature behavior changed.

### KMP Foundation Slice

- shared domain and networking work on Android and iOS;
- one Compose Multiplatform screen runs on both platforms;
- Android Hilt and explicit iOS composition are proven;
- common tests and both platform builds pass in CI;
- architecture documentation and ADRs are published;
- no production credentials are committed.

### Public MVP Core

- Android and iOS support onboarding, Feed, article details, saved articles, basic search, offline cache, and essential settings;
- Room KMP is the source of truth;
- recoverable network failure preserves cached content;
- accessibility and theme behavior are reviewed;
- test and build evidence exists for both platforms.

### Public MVP Complete

- Daily Brief, collections, monitoring, notification preferences, adaptive layouts, and complete offline management are delivered;
- the public repository remains free of private backend, billing, analytics, signing, and deployment secrets.
