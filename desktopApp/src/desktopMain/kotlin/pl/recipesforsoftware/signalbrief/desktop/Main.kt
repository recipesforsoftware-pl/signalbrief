package pl.recipesforsoftware.signalbrief.desktop

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.model.Source
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.Res
import pl.recipesforsoftware.signalbrief.ui.images.installSignalBriefImageLoader
import pl.recipesforsoftware.signalbrief.ui.topheadlines.SignalBriefTheme
import pl.recipesforsoftware.signalbrief.ui.topheadlines.components.ArticleCard

fun main() =
    application {
        var isDarkTheme by remember { mutableStateOf(false) }

        Window(
            onCloseRequest = ::exitApplication,
            title = "SignalBrief",
            state = rememberWindowState(width = 920.dp, height = 720.dp),
        ) {
            SignalBriefTheme(darkTheme = isDarkTheme) {
                installSignalBriefImageLoader()
                DesktopPreview(
                    isDarkTheme = isDarkTheme,
                    onDarkThemeChange = { isDarkTheme = it },
                )
            }
        }
    }

@Composable
private fun DesktopPreview(
    isDarkTheme: Boolean,
    onDarkThemeChange: (Boolean) -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column {
                Text(text = "SignalBrief", style = MaterialTheme.typography.headlineMedium)
                Text(
                    text = "Desktop UI foundation",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Ciemny motyw")
                Switch(checked = isDarkTheme, onCheckedChange = onDarkThemeChange)
            }
        }

        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(desktopPreviewArticles(), key = Article::url) { article ->
                ArticleCard(article = article)
            }
        }
    }
}

private fun desktopPreviewArticles() =
    listOf(
        Article(
            title = "SignalBrief na Compose Desktop",
            description = "Wspólny komponent karty renderuje lokalny zasób Compose.",
            url = "https://example.com/desktop-resource",
            imageUrl = Res.getUri("drawable/sigby_compact.png"),
            source = Source(id = "signalbrief", name = "SignalBrief"),
        ),
        Article(
            title = "Fallback dla nieistniejącego obrazka",
            description = "Celowo błędny lokalny URI sprawdza obsługę błędu bez crasha.",
            url = "https://example.com/missing-resource",
            imageUrl = "file:///signalbrief/missing-thumbnail.png",
            source = Source(id = "preview", name = "Desktop preview"),
        ),
        Article(
            title = "Artykuł bez obrazka",
            description = "Karta zachowuje poprawny układ, gdy obrazek nie jest dostępny.",
            url = "https://example.com/no-image",
            imageUrl = null,
            source = Source(id = "preview", name = "Desktop preview"),
        ),
        Article(
            title = "Wspólne komponenty bez hosta mobilnego",
            description = "Preview korzysta wyłącznie z :core i :shared-ui.",
            url = "https://example.com/shared-components",
            imageUrl = null,
            source = Source(id = "preview", name = "Desktop preview"),
        ),
        Article(
            title = "Stan motywu pozostaje w pamięci",
            description = "Przełącznik nie zapisuje preferencji ani nie wymaga repozytorium.",
            url = "https://example.com/in-memory-theme",
            imageUrl = null,
            source = Source(id = "preview", name = "Desktop preview"),
        ),
        Article(
            title = "Podstawa dla macOS i Windows",
            description = "Windows nadal wymaga osobnej walidacji na właściwym systemie.",
            url = "https://example.com/cross-platform-foundation",
            imageUrl = null,
            source = Source(id = "preview", name = "Desktop preview"),
        ),
    )
