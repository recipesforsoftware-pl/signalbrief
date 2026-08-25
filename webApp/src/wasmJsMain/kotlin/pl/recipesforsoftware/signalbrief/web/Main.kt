package pl.recipesforsoftware.signalbrief.web

import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import pl.recipesforsoftware.signalbrief.ui.app.SignalBriefAppHost
import pl.recipesforsoftware.signalbrief.ui.topheadlines.SignalBriefTheme

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    ComposeViewport(viewportContainerId = "ComposeTarget") {
        val newsRepository = remember { WebNewsRepository() }
        val savedRepository = remember { WebSavedArticlesRepository() }
        SignalBriefTheme { SignalBriefAppHost(newsRepository, savedRepository) }
    }
}
