# SignalBrief Desktop

Ten moduł jest runtime hostem Compose Desktop dla macOS i Windows. Tworzy ręcznie graph danych (`CIO`, Room KMP i repozytoria `:sharedData`) i przekazuje wyłącznie kontrakty repozytoriów do `SignalBriefAppHost`.

Ustaw lokalnie `NEWS_API_KEY` i uruchom na macOS:

```sh
export NEWS_API_KEY="your_news_api_key"
./gradlew :desktopApp:run
```

Klucz nie może trafić do repozytorium. Windows używa `APPDATA`, a macOS `~/Library/Application Support/SignalBrief` dla bazy Room. Nie ma obsługi Linux ani packagingu, signingu lub notarization.
