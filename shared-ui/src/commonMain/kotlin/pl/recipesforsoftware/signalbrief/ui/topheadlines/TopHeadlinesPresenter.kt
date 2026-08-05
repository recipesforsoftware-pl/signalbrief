package pl.recipesforsoftware.signalbrief.ui.topheadlines

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pl.recipesforsoftware.signalbrief.domain.repository.NewsRepository

/** Default country used by the Top Headlines feed when the host does not override it. */
const val DEFAULT_NEWS_COUNTRY: String = "us"

/**
 * Framework-independent state holder for the Top Headlines screen.
 *
 * Depends on the [NewsRepository] contract (never on a concrete Ktor-backed
 * implementation), owns its [CoroutineScope], and exposes immutable state via
 * [uiState]. Callers are responsible for calling [dispose] when the screen is
 * torn down so that in-flight work is cancelled.
 *
 * Concurrency model: a monotonically increasing request generation guards
 * against a stale response overwriting a newer one when concurrent refreshes
 * are issued. Cancellation is always respected: cancelling the owned scope
 * cancels the repository call, which rethrows [kotlin.coroutines.cancellation.CancellationException].
 */
class TopHeadlinesPresenter(
    private val repository: NewsRepository,
    private val country: String = DEFAULT_NEWS_COUNTRY,
    dispatcher: CoroutineDispatcher = Dispatchers.Default,
) {
    private val scope = CoroutineScope(dispatcher + SupervisorJob())

    private val _uiState = MutableStateFlow<TopHeadlinesUiState>(TopHeadlinesUiState.Loading)
    val uiState: StateFlow<TopHeadlinesUiState> = _uiState.asStateFlow()

    private var requestGeneration = 0L

    init {
        refresh()
    }

    /** Loads (or reloads) the top headlines, showing [TopHeadlinesUiState.Loading] while in flight. */
    fun refresh() {
        val generation = ++requestGeneration
        scope.launch {
            _uiState.value = TopHeadlinesUiState.Loading
            val result = repository.getTopHeadlines(country)
            if (generation != requestGeneration) {
                return@launch
            }
            result.fold(
                onSuccess = { feed ->
                    _uiState.value =
                        if (feed.articles.isEmpty()) {
                            TopHeadlinesUiState.Empty
                        } else {
                            TopHeadlinesUiState.Success(feed.articles, feed.source)
                        }
                },
                onFailure = { failure ->
                    _uiState.value = TopHeadlinesUiState.Error(failure.toTopHeadlinesError())
                },
            )
        }
    }

    /** Cancels the owned scope and all in-flight work. Safe to call multiple times. */
    fun dispose() {
        scope.cancel()
    }
}
