package pl.recipesforsoftware.signalbrief.ui.main

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.junit4.v2.createAndroidComposeRule
import androidx.lifecycle.ViewModelProvider
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.compose.KoinIsolatedContext
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.KoinApplication
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pl.recipesforsoftware.signalbrief.di.androidViewModelModule
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.model.Collection
import pl.recipesforsoftware.signalbrief.domain.model.MonitoredTopic
import pl.recipesforsoftware.signalbrief.domain.repository.CollectionsRepository
import pl.recipesforsoftware.signalbrief.domain.repository.NewsRepository
import pl.recipesforsoftware.signalbrief.domain.repository.SavedArticlesRepository
import pl.recipesforsoftware.signalbrief.ui.onboarding.OnboardingPreference
import pl.recipesforsoftware.signalbrief.ui.onboarding.OnboardingViewModel
import pl.recipesforsoftware.signalbrief.ui.theme.ThemePreference
import pl.recipesforsoftware.signalbrief.ui.theme.ThemeViewModel

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class, sdk = [35])
class AndroidViewModelOwnershipTest {
    @get:Rule
    val compose = createAndroidComposeRule<ComponentActivity>()

    private val saved = mockk<SavedArticlesRepository>()
    private val news = mockk<NewsRepository>()
    private val collections = mockk<CollectionsRepository>()
    private val bookmarks = MutableStateFlow(false)
    private val memberships = MutableStateFlow<Set<String>>(emptySet())
    private val cached = MutableStateFlow<List<Article>>(emptyList())
    private val collectionArticles = MutableStateFlow<List<Article>>(emptyList())
    private lateinit var application: KoinApplication

    @Before
    fun setUp() {
        every { saved.observeAllSavedArticles() } returns MutableStateFlow(emptyList())
        every { saved.isArticleSaved(any()) } returns bookmarks
        every { collections.observeAllCollections() } returns MutableStateFlow(emptyList())
        every { collections.observeCollectionIdsForArticle(any()) } returns memberships
        every { collections.observeArticlesInCollection(any()) } returns collectionArticles
        every { news.observeCachedTopHeadlines(any()) } returns cached
        val theme = mockk<ThemePreference>()
        every { theme.isDarkModeEnabled } returns MutableStateFlow(false)
        val onboarding = mockk<OnboardingPreference>()
        every { onboarding.isOnboardingCompleted } returns MutableStateFlow(false)
        application =
            koinApplication {
                modules(
                    androidViewModelModule,
                    module {
                        single { saved }
                        single { collections }
                        single { news }
                        single { theme }
                        single { onboarding }
                    },
                )
            }
    }

    @After
    fun tearDown() {
        application.close()
    }

    @Test
    fun `article pair shares screen lifetime while root VMs belong to Activity`() {
        val article = mutableStateOf(Article("one", null, "https://example.invalid/one", null, null))
        val visible = mutableStateOf(true)
        lateinit var root: MainViewModel
        lateinit var theme: ThemeViewModel
        lateinit var onboarding: OnboardingViewModel
        var count = -1
        compose.setContent {
            KoinIsolatedContext(application) {
                root = koinViewModel()
                theme = koinViewModel()
                onboarding = koinViewModel()
                val state = root.savedArticleCount.collectAsState(initial = 0)
                SideEffect { count = state.value }
                if (visible.value) ArticleDetailsRoute(article.value, {}, {})
            }
        }
        compose.runOnIdle {
            assertSame(root, ViewModelProvider(compose.activity)[MainViewModel::class.java])
            assertSame(theme, ViewModelProvider(compose.activity)[ThemeViewModel::class.java])
            assertSame(onboarding, ViewModelProvider(compose.activity)[OnboardingViewModel::class.java])
            assertEquals(0, count)
            assertEquals(1, bookmarks.subscriptionCount.value)
            assertEquals(1, memberships.subscriptionCount.value)
            article.value = article.value.copy(title = "same identity")
        }
        compose.runOnIdle {
            verify(exactly = 1) { saved.isArticleSaved(any()) }
            verify(exactly = 1) { collections.observeCollectionIdsForArticle(any()) }
            article.value = article.value.copy(url = "https://example.invalid/two")
        }
        compose.runOnIdle {
            verify(exactly = 1) { saved.isArticleSaved("https://example.invalid/two") }
            verify(exactly = 1) { collections.observeCollectionIdsForArticle("https://example.invalid/two") }
            assertEquals(1, bookmarks.subscriptionCount.value)
            assertEquals(1, memberships.subscriptionCount.value)
            visible.value = false
        }
        compose.runOnIdle {
            assertEquals(0, bookmarks.subscriptionCount.value)
            assertEquals(0, memberships.subscriptionCount.value)
            assertSame(root, ViewModelProvider(compose.activity)[MainViewModel::class.java])
            visible.value = true
        }
        compose.runOnIdle {
            verify(exactly = 2) { saved.isArticleSaved("https://example.invalid/two") }
            verify(exactly = 2) { collections.observeCollectionIdsForArticle("https://example.invalid/two") }
        }
    }

    @Test
    fun `Search query changes reuse VM and leaving then returning creates a new one`() {
        val query = mutableStateOf("first")
        val visible = mutableStateOf(true)
        compose.setContent {
            KoinIsolatedContext(application) {
                if (visible.value) SearchRoute(query.value, { query.value = it }, {}, {}, {})
            }
        }
        compose.runOnIdle { query.value = "second" }
        compose.runOnIdle {
            verify(exactly = 1) { news.observeCachedTopHeadlines(any()) }
            visible.value = false
        }
        compose.runOnIdle { visible.value = true }
        compose.runOnIdle {
            verify(exactly = 2) { news.observeCachedTopHeadlines(any()) }
        }
    }

    @Test
    fun `collection and topic IDs recreate their screen owners`() {
        val collection = mutableStateOf(Collection("one", "One"))
        val topic = mutableStateOf(MonitoredTopic("one", "One"))
        compose.setContent {
            KoinIsolatedContext(application) {
                CollectionDetailsRoute(collection.value, {}, {})
                TopicMatchesRoute(topic.value, {}, {})
            }
        }
        compose.runOnIdle {
            collection.value = collection.value.copy(name = "Renamed")
            topic.value = topic.value.copy(query = "Renamed")
        }
        compose.runOnIdle {
            verify(exactly = 1) { collections.observeArticlesInCollection(any()) }
            verify(exactly = 1) { news.observeCachedTopHeadlines(any()) }
            collection.value = Collection("two", "Two")
            topic.value = MonitoredTopic("two", "Two")
        }
        compose.runOnIdle {
            verify(exactly = 1) { collections.observeArticlesInCollection("two") }
            verify(exactly = 2) { news.observeCachedTopHeadlines(any()) }
            assertEquals(1, collectionArticles.subscriptionCount.value)
            assertEquals(1, cached.subscriptionCount.value)
        }
    }
}
