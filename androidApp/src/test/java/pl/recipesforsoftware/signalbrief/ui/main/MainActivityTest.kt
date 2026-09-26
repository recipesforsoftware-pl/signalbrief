package pl.recipesforsoftware.signalbrief.ui.main

import androidx.lifecycle.ViewModelProvider
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import org.junit.After
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.loadKoinModules
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import pl.recipesforsoftware.signalbrief.SignalBriefApplication
import pl.recipesforsoftware.signalbrief.data.remote.NewsApiConfig
import pl.recipesforsoftware.signalbrief.domain.repository.SavedArticlesRepository
import pl.recipesforsoftware.signalbrief.ui.onboarding.OnboardingPreference
import pl.recipesforsoftware.signalbrief.ui.onboarding.OnboardingViewModel
import pl.recipesforsoftware.signalbrief.ui.theme.ThemePreference
import pl.recipesforsoftware.signalbrief.ui.theme.ThemeViewModel

@RunWith(RobolectricTestRunner::class)
@Config(application = SignalBriefApplication::class, sdk = [35])
class MainActivityTest {
    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `Application starts Koin and Activity resolves its three root ViewModels`() {
        val saved = mockk<SavedArticlesRepository>()
        every { saved.observeAllSavedArticles() } returns MutableStateFlow(emptyList())
        val theme = mockk<ThemePreference>()
        every { theme.isDarkModeEnabled } returns MutableStateFlow(false)
        val onboarding = mockk<OnboardingPreference>()
        // Keep the real loading gate active: this smoke test must never request news.
        every { onboarding.isOnboardingCompleted } returns emptyFlow()
        loadKoinModules(
            module {
                single { NewsApiConfig(apiKey = "test-only", baseUrl = "https://example.invalid/") }
                single { saved }
                single { theme }
                single { onboarding }
            },
        )
        Robolectric.buildActivity(MainActivity::class.java).use { controller ->
            controller.setup().visible()
            org.robolectric.Shadows
                .shadowOf(android.os.Looper.getMainLooper())
                .idle()
            val provider = ViewModelProvider(controller.get())
            assertNotNull(provider[ThemeViewModel::class.java])
            assertNotNull(provider[OnboardingViewModel::class.java])
            assertNotNull(provider[MainViewModel::class.java])
        }
    }
}
