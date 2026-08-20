package pl.recipesforsoftware.signalbrief.ui.app

import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Local icon definitions for the bottom navigation bar.
 *
 * Kept locally to avoid pulling the large Material Icons Extended dependency
 * for two simple vectors. Tint is supplied by the caller.
 */
internal object NavigationIcons {
    val Headlines: ImageVector =
        materialIcon(name = "Headlines") {
            materialPath {
                moveTo(4f, 6f)
                horizontalLineToRelative(16f)
                verticalLineToRelative(2f)
                horizontalLineTo(-4f)
                verticalLineToRelative(10f)
                horizontalLineToRelative(-2f)
                verticalLineTo(-10f)
                horizontalLineTo(-6f)
                verticalLineToRelative(10f)
                horizontalLineTo(-4f)
                verticalLineTo(-10f)
                horizontalLineToRelative(-2f)
                close()
                moveTo(4f, 12f)
                horizontalLineToRelative(3f)
                verticalLineToRelative(4f)
                horizontalLineToRelative(6f)
                verticalLineToRelative(-4f)
                horizontalLineToRelative(3f)
                verticalLineToRelative(6f)
                horizontalLineTo(-3f)
                verticalLineToRelative(-2f)
                horizontalLineTo(-6f)
                verticalLineToRelative(2f)
                horizontalLineTo(-3f)
                close()
            }
        }

    val Saved: ImageVector =
        materialIcon(name = "Bookmark") {
            materialPath {
                moveTo(17f, 3f)
                lineTo(7f, 3f)
                curveTo(5.9f, 3f, 5f, 3.9f, 5f, 5f)
                lineTo(5f, 21f)
                lineTo(12f, 18f)
                lineTo(19f, 21f)
                lineTo(19f, 5f)
                curveTo(19f, 3.9f, 18.1f, 3f, 17f, 3f)
                close()
            }
        }
}
