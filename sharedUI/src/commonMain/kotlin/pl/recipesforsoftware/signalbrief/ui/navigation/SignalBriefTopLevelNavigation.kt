package pl.recipesforsoftware.signalbrief.ui.navigation

import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey

/** Replaces the single top-level destination unless [destination] is already selected. */
internal fun NavBackStack<NavKey>.navigateToTopLevelRoute(destination: SignalBriefRoute): Boolean {
    require(destination in signalBriefTopLevelRoutes)

    if (lastOrNull() == destination) {
        return false
    }

    clear()
    add(destination)
    return true
}
