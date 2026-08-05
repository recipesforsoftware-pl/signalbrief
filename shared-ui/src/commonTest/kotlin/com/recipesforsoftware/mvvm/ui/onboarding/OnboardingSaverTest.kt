package com.recipesforsoftware.mvvm.ui.onboarding

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame

class OnboardingSaverTest {
    @Test
    fun `page index survives save and restore`() {
        val presenter = OnboardingPresenter(initialPageIndex = 1)

        val saved = saveOnboardingPageIndex(presenter)
        val restored = restoreOnboardingPageIndex(saved)

        assertEquals(1, saved)
        assertEquals(1, restored.state.value.pageIndex)
        assertNotSame(presenter, restored)
    }

    @Test
    fun `save of a first-page presenter stores zero`() {
        val presenter = OnboardingPresenter()

        assertEquals(0, saveOnboardingPageIndex(presenter))
    }

    @Test
    fun `restored state starts on the saved page`() {
        val restored = restoreOnboardingPageIndex(1)

        assertEquals(1, restored.state.value.pageIndex)
    }
}
