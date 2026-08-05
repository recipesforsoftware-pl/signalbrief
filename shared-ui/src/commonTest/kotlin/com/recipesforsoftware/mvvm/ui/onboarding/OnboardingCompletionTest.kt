package com.recipesforsoftware.mvvm.ui.onboarding

import kotlin.test.Test
import kotlin.test.assertEquals

class OnboardingCompletionTest {
    private fun countingCompletion(): Pair<OnboardingCompletion, () -> Int> {
        var count = 0
        val completion = OnboardingCompletion { count++ }
        return completion to { count }
    }

    @Test
    fun `Skip calls the host completion exactly once`() {
        val (completion, count) = countingCompletion()

        completion.complete()

        assertEquals(1, count())
    }

    @Test
    fun `Start reading calls the host completion exactly once`() {
        val (completion, count) = countingCompletion()

        completion.complete()

        assertEquals(1, count())
    }

    @Test
    fun `Skip then Start reading invoke the host completion exactly once`() {
        val (completion, count) = countingCompletion()

        completion.complete()
        completion.complete()

        assertEquals(1, count())
    }

    @Test
    fun `repeated taps cannot invoke the host completion more than once`() {
        val (completion, count) = countingCompletion()

        repeat(5) { completion.complete() }

        assertEquals(1, count())
    }
}
