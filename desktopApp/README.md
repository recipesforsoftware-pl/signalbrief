# SignalBrief Desktop

This module is the Compose Desktop runtime host for macOS and Windows.

It creates an isolated Koin 4.2.2 graph in `DesktopComposition` for:

- the CIO HTTP client,
- Room KMP,
- `:sharedData` repositories,
- shared ViewModels.

The graph is exposed to Compose through `KoinIsolatedContext`.

`DesktopComposition` explicitly owns and idempotently disposes:

- the HTTP client,
- the Room database,
- the isolated Koin application.

These resources are released after `application {}` terminates.

## Running locally

Set `NEWS_API_KEY` in your shell:

```sh
export NEWS_API_KEY="your_news_api_key"
```

Then run the Desktop application:

```sh
./gradlew :desktopApp:run
```

The API key must not be committed to the repository.

## Application data

The Room database is stored in the platform-specific application data directory.

### macOS

```text
~/Library/Application Support/SignalBrief
```

### Windows

```text
%APPDATA%\SignalBrief
```

## Platform scope

The Desktop target currently supports:

- macOS
- Windows

Linux is not currently supported.

Desktop packaging, signing, and macOS notarization are outside the current project scope.
