package pl.recipesforsoftware.signalbrief.ui.onboarding

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingPresenterTest {
    @Test
    fun `initial page is 0`() =
        runTest(UnconfinedTestDispatcher()) {
            val presenter = OnboardingPresenter()

            assertEquals(0, presenter.state.value.pageIndex)
        }

    @Test
    fun `Continue moves to page 1`() =
        runTest(UnconfinedTestDispatcher()) {
            val presenter = OnboardingPresenter()

            presenter.nextPage()

            assertEquals(1, presenter.state.value.pageIndex)
        }

    @Test
    fun `Back returns to page 0`() =
        runTest(UnconfinedTestDispatcher()) {
            val presenter = OnboardingPresenter()
            presenter.nextPage()

            presenter.previousPage()

            assertEquals(0, presenter.state.value.pageIndex)
        }

    @Test
    fun `Back has no effect on page 1`() =
        runTest(UnconfinedTestDispatcher()) {
            val presenter = OnboardingPresenter()

            presenter.previousPage()

            assertEquals(0, presenter.state.value.pageIndex)
        }

    @Test
    fun `Continue has no effect on page 2`() =
        runTest(UnconfinedTestDispatcher()) {
            val presenter = OnboardingPresenter()
            presenter.nextPage()

            presenter.nextPage()

            assertEquals(1, presenter.state.value.pageIndex)
        }

    @Test
    fun `restored state starts on page 1`() =
        runTest(UnconfinedTestDispatcher()) {
            val presenter = OnboardingPresenter(initialPageIndex = 1)

            assertEquals(1, presenter.state.value.pageIndex)
        }
}
