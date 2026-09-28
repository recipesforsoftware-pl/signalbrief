# SignalBrief Desktop

Ten moduł jest runtime hostem Compose Desktop dla macOS i Windows. Tworzy izolowany graph Koin 4.2.2 w `DesktopComposition` dla `CIO`, Room KMP i repozytoriów `:sharedData`, a następnie przekazuje wyłącznie kontrakty repozytoriów do `SignalBriefAppHost`. `DesktopComposition` jawnie i idempotentnie zwalnia klient HTTP, bazę oraz izolowaną aplikację Koin po zakończeniu `application {}`.

Ustaw lokalnie `NEWS_API_KEY` i uruchom na macOS:

```sh
export NEWS_API_KEY="your_news_api_key"
./gradlew :desktopApp:run
```

Klucz nie może trafić do repozytorium. Windows używa `APPDATA`, a macOS `~/Library/Application Support/SignalBrief` dla bazy Room. Nie ma obsługi Linux ani packagingu, signingu lub notarization.
