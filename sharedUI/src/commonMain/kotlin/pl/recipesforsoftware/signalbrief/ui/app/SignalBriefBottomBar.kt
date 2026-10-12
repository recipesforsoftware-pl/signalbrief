package pl.recipesforsoftware.signalbrief.ui.app

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.Res
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.bottom_nav_brief
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.bottom_nav_daily_brief
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.bottom_nav_headlines
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.bottom_nav_saved
import pl.recipesforsoftware.signalbrief.ui.navigation.SignalBriefRoute

@Composable
internal fun SignalBriefBottomBar(
    currentDestination: SignalBriefRoute,
    onNavigate: (SignalBriefRoute) -> Unit,
    savedArticleCount: Int,
) {
    NavigationBar {
        NavigationBarItem(
            selected = currentDestination == SignalBriefRoute.Headlines,
            onClick = { onNavigate(SignalBriefRoute.Headlines) },
            icon = {
                Icon(
                    imageVector = NavigationIcons.Headlines,
                    contentDescription = null,
                )
            },
            label = { Text(stringResource(Res.string.bottom_nav_headlines)) },
            colors = bottomBarColors(),
        )
        NavigationBarItem(
            selected = currentDestination == SignalBriefRoute.DailyBrief,
            onClick = { onNavigate(SignalBriefRoute.DailyBrief) },
            icon = {
                Icon(
                    imageVector = NavigationIcons.Brief,
                    contentDescription = stringResource(Res.string.bottom_nav_daily_brief),
                )
            },
            label = { Text(stringResource(Res.string.bottom_nav_brief)) },
            colors = bottomBarColors(),
        )
        SavedNavigationBarItem(currentDestination, onNavigate, savedArticleCount)
    }
}

@Composable
private fun bottomBarColors() =
    NavigationBarItemDefaults.colors(
        selectedIconColor = MaterialTheme.colorScheme.onSecondaryContainer,
        selectedTextColor = MaterialTheme.colorScheme.onSurface,
        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
    )

@Composable
private fun RowScope.SavedNavigationBarItem(
    currentDestination: SignalBriefRoute,
    onNavigate: (SignalBriefRoute) -> Unit,
    savedArticleCount: Int,
) {
    NavigationBarItem(
        selected = currentDestination == SignalBriefRoute.Saved,
        onClick = { onNavigate(SignalBriefRoute.Saved) },
        icon = {
            BadgedBox(badge = { savedCountBadgeLabel(savedArticleCount)?.let { Badge { Text(it) } } }) {
                Icon(
                    imageVector = NavigationIcons.Saved,
                    contentDescription = null,
                )
            }
        },
        label = { Text(stringResource(Res.string.bottom_nav_saved)) },
        colors = bottomBarColors(),
    )
}

/** Returns the compact Saved navigation badge label, or `null` when it should be hidden. */
internal fun savedCountBadgeLabel(savedArticleCount: Int): String? =
    when {
        savedArticleCount <= 0 -> null
        savedArticleCount > MAX_SAVED_COUNT_BADGE -> "$MAX_SAVED_COUNT_BADGE+"
        else -> savedArticleCount.toString()
    }

private const val MAX_SAVED_COUNT_BADGE = 99
