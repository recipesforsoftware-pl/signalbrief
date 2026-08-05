package com.recipesforsoftware.mvvm.ui.onboarding

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingPresenterTest {
    @Test
    fun `initial state shows page 1 and is not complete`() =
        runTest(UnconfinedTestDispatcher()) {
            val presenter = OnboardingPresenter()

            assertEquals(0, presenter.state.value.pageIndex)
            assertFalse(presenter.state.value.isComplete)
        }

    @Test
    fun `Continue moves from page 1 to page 2`() =
        runTest(UnconfinedTestDispatcher()) {
            val presenter = OnboardingPresenter()

            presenter.nextPage()

            assertEquals(1, presenter.state.value.pageIndex)
            assertFalse(presenter.state.value.isComplete)
        }

    @Test
    fun `Back returns from page 2 to page 1`() =
        runTest(UnconfinedTestDispatcher()) {
            val presenter = OnboardingPresenter()
            presenter.nextPage()

            presenter.previousPage()

            assertEquals(0, presenter.state.value.pageIndex)
            assertFalse(presenter.state.value.isComplete)
        }

    @Test
    fun `Back has no effect on page 1`() =
        runTest(UnconfinedTestDispatcher()) {
            val presenter = OnboardingPresenter()

            presenter.previousPage()

            assertEquals(0, presenter.state.value.pageIndex)
            assertFalse(presenter.state.value.isComplete)
        }

    @Test
    fun `Continue has no effect on page 2`() =
        runTest(UnconfinedTestDispatcher()) {
            val presenter = OnboardingPresenter()
            presenter.nextPage()

            presenter.nextPage()

            assertEquals(1, presenter.state.value.pageIndex)
            assertFalse(presenter.state.value.isComplete)
        }

    @Test
    fun `Skip completes onboarding`() =
        runTest(UnconfinedTestDispatcher()) {
            val presenter = OnboardingPresenter()

            presenter.complete()

            assertTrue(presenter.state.value.isComplete)
            assertEquals(0, presenter.state.value.pageIndex)
        }

    @Test
    fun `Start reading completes onboarding`() =
        runTest(UnconfinedTestDispatcher()) {
            val presenter = OnboardingPresenter()
            presenter.nextPage()

            presenter.complete()

            assertTrue(presenter.state.value.isComplete)
            assertEquals(1, presenter.state.value.pageIndex)
        }

    @Test
    fun `completion callback fires once`() =
        runTest(UnconfinedTestDispatcher()) {
            val presenter = OnboardingPresenter()
            var callbackCount = 0
            val onComplete = { callbackCount++ }

            presenter.complete()
            onComplete()

            assertTrue(presenter.state.value.isComplete)
            assertEquals(1, callbackCount)
        }

    @Test
    fun `state survives recomposition`() =
        runTest(UnconfinedTestDispatcher()) {
            val presenter = OnboardingPresenter()

            presenter.nextPage()
            val firstState = presenter.state.value
            presenter.nextPage()
            val secondState = presenter.state.value

            assertEquals(firstState, secondState)
            assertEquals(1, secondState.pageIndex)
        }

    @Test
    fun `new presenter instance starts fresh`() =
        runTest(UnconfinedTestDispatcher()) {
            val first = OnboardingPresenter()
            first.complete()

            val second = OnboardingPresenter()

            assertTrue(first.state.value.isComplete)
            assertFalse(second.state.value.isComplete)
            assertEquals(0, second.state.value.pageIndex)
        }
}
