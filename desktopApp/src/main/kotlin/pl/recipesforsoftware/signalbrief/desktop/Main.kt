package pl.recipesforsoftware.signalbrief.desktop

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import org.koin.compose.KoinIsolatedContext
import pl.recipesforsoftware.signalbrief.ui.app.SignalBriefAppHost
import pl.recipesforsoftware.signalbrief.ui.topheadlines.SignalBriefTheme

fun main() {
    val composition =
        createDesktopComposition(
            apiKey = readNewsApiKey(),
            databasePath = createDesktopDatabasePath(),
        )

    try {
        application {
            Window(
                onCloseRequest = ::exitApplication,
                title = "SignalBrief",
                state =
                    rememberWindowState(
                        width = 920.dp,
                        height = 720.dp,
                    ),
            ) {
                KoinIsolatedContext(context = composition.koinApplication) {
                    SignalBriefTheme {
                        SignalBriefAppHost()
                    }
                }
            }
        }
    } finally {
        composition.dispose()
    }
}
