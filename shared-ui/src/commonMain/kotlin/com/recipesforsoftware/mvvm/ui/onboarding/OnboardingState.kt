package com.recipesforsoftware.mvvm.ui.onboarding

/**
 * Renderable state of the two-page onboarding flow.
 *
 * @property pageIndex Zero-based page index: 0 for the first page, 1 for the second.
 */
data class OnboardingState(
    val pageIndex: Int = 0,
) {
    init {
        require(pageIndex in 0..MAX_PAGE_INDEX) {
            "pageIndex must be between 0 and $MAX_PAGE_INDEX, was $pageIndex"
        }
    }

    companion object {
        /** Total number of onboarding pages. */
        const val PAGE_COUNT: Int = 2

        /** Highest valid page index. */
        const val MAX_PAGE_INDEX: Int = PAGE_COUNT - 1
    }
}
