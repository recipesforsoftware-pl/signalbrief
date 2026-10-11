package pl.recipesforsoftware.signalbrief.ui.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SignalBriefTopLevelNavigationTest {
    @Test
    fun startsAtHeadlines() {
        val backStack = topLevelBackStack()

        assertEquals(listOf<NavKey>(SignalBriefRoute.Headlines), backStack.toList())
    }

    @Test
    fun replacesTopLevelDestinationsInBottomBarOrder() {
        val backStack = topLevelBackStack()

        assertTrue(backStack.navigateToTopLevelRoute(SignalBriefRoute.DailyBrief))
        assertEquals(listOf<NavKey>(SignalBriefRoute.DailyBrief), backStack.toList())

        assertTrue(backStack.navigateToTopLevelRoute(SignalBriefRoute.Saved))
        assertEquals(listOf<NavKey>(SignalBriefRoute.Saved), backStack.toList())

        assertTrue(backStack.navigateToTopLevelRoute(SignalBriefRoute.Headlines))
        assertEquals(listOf<NavKey>(SignalBriefRoute.Headlines), backStack.toList())
    }

    @Test
    fun reselectingCurrentDestinationLeavesBackStackUntouched() {
        val backStack = topLevelBackStack()
        val currentEntry = backStack.single()

        assertFalse(backStack.navigateToTopLevelRoute(SignalBriefRoute.Headlines))

        assertEquals(listOf(currentEntry), backStack.toList())
    }

    private fun topLevelBackStack(): NavBackStack<NavKey> = NavBackStack(signalBriefStartRoute)
}
