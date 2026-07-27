package com.recipesforsoftware.mvvm

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import com.recipesforsoftware.mvvm.ui.theme.ThemePreference
import com.recipesforsoftware.mvvm.ui.theme.ThemeViewModel
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ThemeViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var themePreference: ThemePreference
    private lateinit var viewModel: ThemeViewModel

    private val darkModeFlow = MutableStateFlow(false)

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        themePreference = mockk(relaxed = true)
        coEvery { themePreference.isDarkModeEnabled } returns darkModeFlow
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): ThemeViewModel {
        return ThemeViewModel(themePreference)
    }

    @Test
    fun `initial state is false when preference is false`() = runTest {
        // Given
        darkModeFlow.value = false
        viewModel = createViewModel()

        viewModel.isDarkMode.test {
            // First emission is initialValue from stateIn, then upstream
            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `initial state reflects preference true after collection`() = runTest {
        // Given
        darkModeFlow.value = true
        viewModel = createViewModel()

        viewModel.isDarkMode.test {
            // stateIn emits initialValue first, then upstream value
            assertFalse(awaitItem()) // initialValue = false
            assertTrue(awaitItem())  // upstream value = true
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggleDarkMode when off calls setDarkMode with true`() = runTest {
        // Given
        darkModeFlow.value = false
        viewModel = createViewModel()

        // When
        viewModel.toggleDarkMode()
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { themePreference.setDarkMode(true) }
    }

    @Test
    fun `toggleDarkMode when on calls setDarkMode with false`() = runTest {
        // Given
        darkModeFlow.value = true
        viewModel = createViewModel()

        // When
        viewModel.toggleDarkMode()
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { themePreference.setDarkMode(false) }
    }

    @Test
    fun `setDarkMode true calls preference with true`() = runTest {
        // Given
        viewModel = createViewModel()

        // When
        viewModel.setDarkMode(true)
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { themePreference.setDarkMode(true) }
    }

    @Test
    fun `setDarkMode false calls preference with false`() = runTest {
        // Given
        viewModel = createViewModel()

        // When
        viewModel.setDarkMode(false)
        advanceUntilIdle()

        // Then
        coVerify(exactly = 1) { themePreference.setDarkMode(false) }
    }

    @Test
    fun `isDarkMode emits updated value when preference changes`() = runTest {
        // Given
        viewModel = createViewModel()

        viewModel.isDarkMode.test {
            // Initial: initialValue = false
            assertFalse(awaitItem())

            // When - preference flow emits new value
            darkModeFlow.value = true

            // Then - upstream value
            assertTrue(awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `toggleDarkMode multiple times calls preference each time`() = runTest {
        // Given
        darkModeFlow.value = false
        viewModel = createViewModel()

        // When
        viewModel.toggleDarkMode()
        advanceUntilIdle()
        viewModel.toggleDarkMode()
        advanceUntilIdle()
        viewModel.toggleDarkMode()
        advanceUntilIdle()

        // Then
        coVerify(exactly = 3) { themePreference.setDarkMode(any()) }
    }
}
