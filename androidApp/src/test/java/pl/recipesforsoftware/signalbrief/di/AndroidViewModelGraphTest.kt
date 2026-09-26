package pl.recipesforsoftware.signalbrief.di

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.koin.core.KoinApplication
import org.koin.core.parameter.parametersOf
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import org.koin.viewmodel.factory.KoinViewModelFactory
import pl.recipesforsoftware.signalbrief.domain.model.Article
import pl.recipesforsoftware.signalbrief.domain.model.Collection
import pl.recipesforsoftware.signalbrief.domain.model.MonitoredTopic
import pl.recipesforsoftware.signalbrief.domain.repository.CollectionsRepository
import pl.recipesforsoftware.signalbrief.domain.repository.NewsRepository
import pl.recipesforsoftware.signalbrief.domain.repository.SavedArticlesRepository
import pl.recipesforsoftware.signalbrief.domain.repository.TopicMonitoringRepository
import pl.recipesforsoftware.signalbrief.ui.articledetails.ArticleCollectionAssignmentViewModel
import pl.recipesforsoftware.signalbrief.ui.articledetails.ArticleDetailsViewModel
import pl.recipesforsoftware.signalbrief.ui.collectiondetails.CollectionDetailsViewModel
import pl.recipesforsoftware.signalbrief.ui.collections.CollectionsViewModel
import pl.recipesforsoftware.signalbrief.ui.dailybrief.DailyBriefViewModel
import pl.recipesforsoftware.signalbrief.ui.main.MainViewModel
import pl.recipesforsoftware.signalbrief.ui.onboarding.OnboardingPreference
import pl.recipesforsoftware.signalbrief.ui.onboarding.OnboardingViewModel
import pl.recipesforsoftware.signalbrief.ui.saved.SavedArticlesViewModel
import pl.recipesforsoftware.signalbrief.ui.search.SearchViewModel
import pl.recipesforsoftware.signalbrief.ui.settings.SettingsViewModel
import pl.recipesforsoftware.signalbrief.ui.theme.ThemePreference
import pl.recipesforsoftware.signalbrief.ui.theme.ThemeViewModel
import pl.recipesforsoftware.signalbrief.ui.topheadlines.TopHeadlinesViewModel
import pl.recipesforsoftware.signalbrief.ui.topicmonitoring.TopicMatchesViewModel
import pl.recipesforsoftware.signalbrief.ui.topicmonitoring.TopicMonitoringViewModel
import kotlin.reflect.KClass

@OptIn(ExperimentalCoroutinesApi::class, org.koin.core.annotation.KoinInternalApi::class)
class AndroidViewModelGraphTest {
    private val dispatcher = StandardTestDispatcher()
    private val stores = mutableListOf<ViewModelStore>()
    private val saved = mockk<SavedArticlesRepository>()
    private val news = mockk<NewsRepository>()
    private val collections = mockk<CollectionsRepository>()
    private val topics = mockk<TopicMonitoringRepository>()
    private val savedFlow = MutableStateFlow<List<Article>>(emptyList())
    private val bookmarkFlow = MutableStateFlow(false)
    private val membershipFlow = MutableStateFlow<Set<String>>(emptySet())
    private lateinit var application: KoinApplication

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        every { saved.observeAllSavedArticles() } returns savedFlow
        every { saved.isArticleSaved(any()) } returns bookmarkFlow
        every { news.observeCachedTopHeadlines(any()) } returns MutableStateFlow(emptyList())
        coEvery { news.getTopHeadlines(any()) } returns Result.failure(IllegalStateException("offline test"))
        every { collections.observeAllCollections() } returns MutableStateFlow(emptyList())
        every { collections.observeArticlesInCollection(any()) } returns MutableStateFlow(emptyList())
        every { collections.observeCollectionIdsForArticle(any()) } returns membershipFlow
        every { topics.observeTopics() } returns MutableStateFlow(emptyList())
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
                        single { news }
                        single { collections }
                        single { topics }
                        single { theme }
                        single { onboarding }
                    },
                )
            }
    }

    @After
    fun tearDown() {
        stores.forEach { it.clear() }
        application.close()
        Dispatchers.resetMain()
    }

    private fun store() = ViewModelStore().also(stores::add)

    private fun <T : ViewModel> resolve(
        type: KClass<T>,
        store: ViewModelStore,
        parameter: Any? = null,
    ): T =
        ViewModelProvider.create(
            store,
            KoinViewModelFactory(type, application.koin.scopeRegistry.rootScope, params = { parametersOf(parameter) }),
        )[type]

    @Test
    fun `all fourteen definitions resolve through AndroidX stores and reuse their owner`() =
        runTest(dispatcher) {
            val owner = store()
            val definitions =
                listOf(
                    ThemeViewModel::class,
                    OnboardingViewModel::class,
                    MainViewModel::class,
                    TopHeadlinesViewModel::class,
                    SavedArticlesViewModel::class,
                    CollectionsViewModel::class,
                    DailyBriefViewModel::class,
                    TopicMonitoringViewModel::class,
                    SettingsViewModel::class,
                )
            definitions.forEach { type -> assertSame(resolve(type, owner), resolve(type, owner)) }
            checkParameters(owner, "one")
            runCurrent()
            owner.clear()
            runCurrent()
            assertEquals(0, savedFlow.subscriptionCount.value)
            assertEquals(0, bookmarkFlow.subscriptionCount.value)
            assertEquals(0, membershipFlow.subscriptionCount.value)
        }

    @Test
    fun `runtime snapshots seed new stores but changed parameters never replace an existing VM`() =
        runTest(dispatcher) {
            val first = store()
            val search = resolve(SearchViewModel::class, first, "first")
            assertSame(search, resolve(SearchViewModel::class, first, "changed"))
            assertEquals("first", search.query.value)
            checkParameters(first, "one")
            val second = store()
            assertNotSame(search, resolve(SearchViewModel::class, second, "second"))
            assertEquals("second", resolve(SearchViewModel::class, second, "ignored").query.value)
            checkParameters(second, "two")
            runCurrent()
            for (id in listOf("one", "two")) {
                verify { saved.isArticleSaved("https://example.invalid/$id") }
                verify { collections.observeCollectionIdsForArticle("https://example.invalid/$id") }
                verify { collections.observeArticlesInCollection(id) }
            }
        }

    private fun checkParameters(
        owner: ViewModelStore,
        id: String,
    ) {
        val article = Article(id, null, "https://example.invalid/$id", null, null)
        val collection = Collection(id, "Collection $id")
        val topic = MonitoredTopic(id, "Query $id")
        val details = resolve(ArticleDetailsViewModel::class, owner, article)
        assertEquals(article, details.uiState.value.article)
        assertSame(details, resolve(ArticleDetailsViewModel::class, owner, article.copy(url = "changed")))
        val assignment = resolve(ArticleCollectionAssignmentViewModel::class, owner, article)
        assertSame(assignment, resolve(ArticleCollectionAssignmentViewModel::class, owner, article))
        assertEquals(collection, resolve(CollectionDetailsViewModel::class, owner, collection).uiState.value.collection)
        assertEquals(
            topic,
            (
                resolve(
                    TopicMatchesViewModel::class,
                    owner,
                    topic,
                ).uiState.value as pl.recipesforsoftware.signalbrief.ui.topicmonitoring.TopicMatchesUiState.Loading
            ).topic,
        )
        resolve(SearchViewModel::class, owner, id)
    }
}
