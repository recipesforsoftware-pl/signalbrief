package pl.recipesforsoftware.signalbrief.ui.app

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.ui.images.installSignalBriefImageLoader
import pl.recipesforsoftware.signalbrief.ui.onboarding.OnboardingCompletion
import pl.recipesforsoftware.signalbrief.ui.onboarding.OnboardingScreen
import pl.recipesforsoftware.signalbrief.ui.onboarding.rememberOnboardingPresenter

/**
 * Shared application shell for SignalBrief.
 *
 * Decides between the two-page onboarding flow and the main three-destination
 * app based on [onboardingCompleted]:
 * - `null`  -> the persisted value is still loading; a subtle loading indicator
 *              is shown to avoid an onboarding flash.
 * - `false` -> onboarding is shown.
 * - `true`  -> the host-provided destination content with a three-item bottom
 *              navigation bar (Headlines / Brief / Saved) is shown.
 *
 * The shell owns both navigation levels. Top-level navigation is exactly the
 * three [AppDestination] entries, kept in `rememberSaveable` with an explicit
 * [Saver]; it survives host recreation and defaults to
 * [AppDestination.Headlines]. Child navigation is a single nullable selected
 * article: when set, Article Details replaces the destination content and the
 * bottom bar disappears (details is a child screen, not a third tab), while
 * [currentDestination] keeps holding the originating destination so Back
 * returns to it exactly. Search is a child screen of Headlines and is tracked
 * by [isSearchVisible]; it also survives recreation through `rememberSaveable`.
 *
 * Navigation priority while the main app is visible:
 * 1. [selectedArticle] -> Article Details.
 * 2. [isSearchVisible] -> Search.
 * 3. [currentDestination] -> Headlines, Daily Brief, or Saved.
 *
 * Toolbar back and any host-integrated system back both funnel through the
 * same state clear, so there is one shared transition path and no back stack.
 * The selected article survives recreation through [SelectedArticleSaver].
 *
 * The shell also installs the shared Coil image-loader singleton once for the
 * app composition root. Both "Skip" and "Start reading" funnel through an
 * [OnboardingCompletion] guard so [onCompleteOnboarding] fires at most once
 * per shell instance; the host persists the outcome itself.
 */
typealias TopHeadlinesContent =
    @Composable (
        bottomBar: @Composable () -> Unit,
        onArticleClick: (Article) -> Unit,
        onSearchClick: () -> Unit,
    ) -> Unit

typealias SavedContent =
    @Composable (
        bottomBar: @Composable () -> Unit,
        onArticleClick: (Article) -> Unit,
        onCollectionsClick: () -> Unit,
    ) -> Unit

typealias CollectionsContent = @Composable (onBack: () -> Unit) -> Unit

typealias DailyBriefContent =
    @Composable (
        bottomBar: @Composable () -> Unit,
        onArticleClick: (Article) -> Unit,
    ) -> Unit

typealias SearchContent =
    @Composable (
        initialQuery: String,
        onQueryChange: (String) -> Unit,
        onArticleClick: (Article) -> Unit,
        onBack: () -> Unit,
    ) -> Unit

typealias ArticleDetailsContent =
    @Composable (
        article: Article,
        onBack: () -> Unit,
    ) -> Unit

@Composable
fun SignalBriefApp(
    onboardingCompleted: Boolean?,
    onCompleteOnboarding: () -> Unit,
    topHeadlinesContent: TopHeadlinesContent,
    savedContent: SavedContent,
    searchContent: SearchContent,
    articleDetailsContent: ArticleDetailsContent,
    dailyBriefContent: DailyBriefContent,
    collectionsContent: CollectionsContent = {},
    savedArticleCount: Int = 0,
    modifier: Modifier = Modifier,
) {
    installSignalBriefImageLoader()
    Box(modifier = modifier.fillMaxSize()) {
        AnimatedVisibility(
            visible = onboardingCompleted == null,
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            InitializingContent()
        }

        if (onboardingCompleted == false) {
            val onboardingPresenter = rememberOnboardingPresenter()
            val completion = remember { OnboardingCompletion(onCompleteOnboarding) }

            OnboardingScreen(
                presenter = onboardingPresenter,
                onSkip = completion::complete,
                onComplete = completion::complete,
            )
        }

        if (onboardingCompleted == true) {
            SignalBriefMainContent(
                topHeadlinesContent = topHeadlinesContent,
                dailyBriefContent = dailyBriefContent,
                savedContent = savedContent,
                searchContent = searchContent,
                articleDetailsContent = articleDetailsContent,
                collectionsContent = collectionsContent,
                savedArticleCount = savedArticleCount,
            )
        }
    }
}

@Composable
private fun SignalBriefMainContent(
    topHeadlinesContent: TopHeadlinesContent,
    dailyBriefContent: DailyBriefContent,
    savedContent: SavedContent,
    searchContent: SearchContent,
    articleDetailsContent: ArticleDetailsContent,
    collectionsContent: CollectionsContent,
    savedArticleCount: Int,
) {
    var currentDestination by rememberSaveable(stateSaver = AppDestinationSaver) {
        mutableStateOf(AppDestination.Headlines)
    }
    var selectedArticle by rememberSaveable(stateSaver = SelectedArticleSaver) {
        mutableStateOf<Article?>(null)
    }
    var isSearchVisible by rememberSaveable {
        mutableStateOf(false)
    }
    var isCollectionsVisible by rememberSaveable { mutableStateOf(false) }
    var searchQuery by rememberSaveable {
        mutableStateOf("")
    }

    if (selectedArticle != null) {
        articleDetailsContent(requireNotNull(selectedArticle)) { selectedArticle = null }
    } else if (isSearchVisible) {
        searchContent(
            searchQuery,
            { searchQuery = it },
            { selectedArticle = it },
            { isSearchVisible = false },
        )
    } else if (isCollectionsVisible) {
        collectionsContent { isCollectionsVisible = false }
    } else {
        val bottomBar: @Composable () -> Unit = {
            SignalBriefBottomBar(
                currentDestination = currentDestination,
                onNavigate = { currentDestination = it },
                savedArticleCount = savedArticleCount,
            )
        }

        when (currentDestination) {
            AppDestination.Headlines -> {
                topHeadlinesContent(
                    bottomBar,
                    { article -> selectedArticle = article },
                    { isSearchVisible = true },
                )
            }

            AppDestination.DailyBrief -> {
                dailyBriefContent(bottomBar) { article -> selectedArticle = article }
            }

            AppDestination.Saved -> {
                savedContent(
                    bottomBar,
                    { article -> selectedArticle = article },
                    { isCollectionsVisible = true },
                )
            }
        }
    }
}

@Composable
private fun SignalBriefBottomBar(
    currentDestination: AppDestination,
    onNavigate: (AppDestination) -> Unit,
    savedArticleCount: Int,
) {
    NavigationBar {
        NavigationBarItem(
            selected = currentDestination == AppDestination.Headlines,
            onClick = { onNavigate(AppDestination.Headlines) },
            icon = {
                Icon(
                    imageVector = NavigationIcons.Headlines,
                    contentDescription = null,
                )
            },
            label = { Text("Headlines") },
            colors =
                NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
        )
        NavigationBarItem(
            selected = currentDestination == AppDestination.DailyBrief,
            onClick = { onNavigate(AppDestination.DailyBrief) },
            icon = {
                Icon(
                    imageVector = NavigationIcons.Brief,
                    contentDescription = "Daily Brief",
                )
            },
            label = { Text("Brief") },
            colors =
                NavigationBarItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                ),
        )
        SavedNavigationBarItem(currentDestination, onNavigate, savedArticleCount)
    }
}

@Composable
private fun RowScope.SavedNavigationBarItem(
    currentDestination: AppDestination,
    onNavigate: (AppDestination) -> Unit,
    savedArticleCount: Int,
) {
    NavigationBarItem(
        selected = currentDestination == AppDestination.Saved,
        onClick = { onNavigate(AppDestination.Saved) },
        icon = {
            BadgedBox(
                badge = {
                    savedCountBadgeLabel(savedArticleCount)?.let { label ->
                        Badge { Text(label) }
                    }
                },
            ) {
                Icon(
                    imageVector = NavigationIcons.Saved,
                    contentDescription = null,
                )
            }
        },
        label = { Text("Saved") },
        colors =
            NavigationBarItemDefaults.colors(
                selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
                selectedTextColor = MaterialTheme.colorScheme.onSurface,
                indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
            ),
    )
}

/** Returns the compact Saved navigation badge label, or `null` when it should be hidden. */
internal fun savedCountBadgeLabel(savedArticleCount: Int): String? =
    when {
        savedArticleCount <= 0 -> null
        savedArticleCount > MAX_SAVED_COUNT_BADGE -> "$MAX_SAVED_COUNT_BADGE+"
        else -> savedArticleCount.toString()
    }

private const val MAX_SAVED_COUNT_BADGE = 99

@Composable
private fun InitializingContent() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(48.dp),
            color = MaterialTheme.colorScheme.primary,
            trackColor = MaterialTheme.colorScheme.surfaceVariant,
        )
    }
}

/**
 * Compose [Saver] that persists the selected [AppDestination] as its enum name.
 *
 * The enum is not JVM-serializable and must not assume Android-only save/restore
 * semantics; saving the stable name and restoring by lookup keeps the strategy
 * compatible with Compose Multiplatform.
 */
internal val AppDestinationSaver: Saver<AppDestination, String> =
    Saver(
        save = { destination -> destination.name },
        restore = { name -> AppDestination.entries.find { it.name == name } },
    )
