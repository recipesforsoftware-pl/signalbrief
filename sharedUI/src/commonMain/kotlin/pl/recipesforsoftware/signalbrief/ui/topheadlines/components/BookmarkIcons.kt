package pl.recipesforsoftware.signalbrief.ui.topheadlines.components

import androidx.compose.material.icons.materialIcon
import androidx.compose.material.icons.materialPath
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Local bookmark icon definitions used by [ArticleCard].
 *
 * Kept locally to avoid pulling the large Material Icons Extended dependency
 * for two simple vectors. Tint is supplied by the caller.
 */
internal object BookmarkIcons {
    val Filled: ImageVector =
        materialIcon(name = "BookmarkFilled") {
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

    val Outlined: ImageVector =
        materialIcon(name = "BookmarkOutlined") {
            materialPath {
                moveTo(17f, 3f)
                lineTo(7f, 3f)
                curveTo(5.9f, 3f, 5.01f, 3.9f, 5.01f, 5f)
                lineTo(5f, 21f)
                lineTo(12f, 18f)
                lineTo(19f, 21f)
                lineTo(19f, 5f)
                curveTo(19f, 3.9f, 18.1f, 3f, 17f, 3f)
                close()

                moveTo(17f, 18f)
                lineTo(12f, 15.82f)
                lineTo(7f, 18f)
                lineTo(7f, 5f)
                lineTo(17f, 5f)
                close()
            }
        }
}
