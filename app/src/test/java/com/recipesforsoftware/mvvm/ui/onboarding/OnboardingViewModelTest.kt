package com.recipesforsoftware.mvvm.ui.onboarding

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class OnboardingViewModelTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var onboardingPreference: OnboardingPreference
    private lateinit var viewModel: OnboardingViewModel

    private val completedFlow = MutableStateFlow<Boolean?>(null)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        onboardingPreference = mockk(relaxed = true)
        coEvery { onboardingPreference.isOnboardingCompleted } returns completedFlow.filterNotNull()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): OnboardingViewModel = OnboardingViewModel(onboardingPreference)

    @Test
    fun `initial state is null until preference emits`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()

            viewModel.isOnboardingCompleted.test {
                assertNull(awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `state reflects preference false after collection`() =
        runTest(testDispatcher) {
            completedFlow.value = false
            viewModel = createViewModel()

            viewModel.isOnboardingCompleted.test {
                assertNull(awaitItem()) // initialValue = null
                assertEquals(false, awaitItem()) // upstream value
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `state reflects preference true after collection`() =
        runTest(testDispatcher) {
            completedFlow.value = true
            viewModel = createViewModel()

            viewModel.isOnboardingCompleted.test {
                assertNull(awaitItem()) // initialValue = null
                assertEquals(true, awaitItem()) // upstream value
                cancelAndIgnoreRemainingEvents()
            }
        }

    @Test
    fun `completeOnboarding persists true`() =
        runTest(testDispatcher) {
            viewModel = createViewModel()

            viewModel.completeOnboarding()
            advanceUntilIdle()

            coVerify(exactly = 1) { onboardingPreference.setOnboardingCompleted(true) }
        }

    @Test
    fun `emits updated value when preference changes`() =
        runTest(testDispatcher) {
            completedFlow.value = false
            viewModel = createViewModel()

            viewModel.isOnboardingCompleted.test {
                assertNull(awaitItem()) // initialValue
                assertEquals(false, awaitItem()) // upstream

                completedFlow.value = true

                assertEquals(true, awaitItem())
                cancelAndIgnoreRemainingEvents()
            }
        }
}
