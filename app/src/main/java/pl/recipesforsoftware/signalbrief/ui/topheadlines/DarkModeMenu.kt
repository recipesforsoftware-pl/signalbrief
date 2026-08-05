package pl.recipesforsoftware.signalbrief.ui.topheadlines

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.DarkMode
import androidx.compose.material.icons.rounded.LightMode
import androidx.compose.material.icons.rounded.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp

/**
 * Android-only settings menu rendered inside the shared top bar via the
 * `topBarActions` slot of [TopHeadlinesScreen]. Lives in the app layer because
 * it depends on the extended icon set and the persisted dark-mode preference.
 */
@Composable
fun DarkModeMenu(
    isDarkMode: Boolean,
    onToggleDarkMode: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var menuExpanded by remember { mutableStateOf(false) }
    val themeIcon: ImageVector = if (isDarkMode) Icons.Rounded.DarkMode else Icons.Rounded.LightMode

    Box(modifier = modifier) {
        IconButton(onClick = { menuExpanded = true }) {
            Icon(
                imageVector = Icons.Rounded.MoreVert,
                contentDescription = "Menu",
            )
        }
        DropdownMenu(
            expanded = menuExpanded,
            onDismissRequest = { menuExpanded = false },
        ) {
            DropdownMenuItem(
                text = { Text("Dark Mode") },
                leadingIcon = {
                    Icon(
                        imageVector = themeIcon,
                        contentDescription = null,
                    )
                },
                trailingIcon = {
                    Switch(
                        checked = isDarkMode,
                        onCheckedChange = {
                            onToggleDarkMode()
                            menuExpanded = false
                        },
                        thumbContent = {
                            Icon(
                                imageVector = themeIcon,
                                contentDescription = null,
                                modifier = Modifier.size(SwitchDefaults.IconSize),
                            )
                        },
                        colors =
                            SwitchDefaults.colors(
                                checkedTrackColor = MaterialTheme.colorScheme.primary,
                                checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            ),
                    )
                },
                onClick = {
                    onToggleDarkMode()
                    menuExpanded = false
                },
            )
        }
    }
}
