# SignalBrief Desktop foundation

Ten moduł stanowi fundament kompilacji i wspólnego UI w Compose Desktop dla przyszłej obsługi macOS i Windows.

Uruchom lokalnie:

```sh
./gradlew :desktopApp:run
```

Okno pokazuje wspólny `SignalBriefTheme`, przełącznik jasnego/ciemnego motywu oraz przewijaną listę `ArticleCard`: lokalny Compose Resource, celowo błędną referencję testującą fallback i kartę bez obrazka.

Nie ma tu jeszcze networku, Room ani produkcyjnej kompozycji. Windows wymaga oddzielnej walidacji na systemie Windows.
