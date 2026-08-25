package pl.recipesforsoftware.signalbrief.ui.topheadlines.components

import androidx.compose.foundation.Image
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.Res
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.demo_repair_cafe
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.demo_roof_garden

private const val ROOF_GARDEN_REFERENCE = "demo-resource://roof-garden"
private const val REPAIR_CAFE_REFERENCE = "demo-resource://repair-cafe"

@Composable
internal actual fun SignalBriefArticleThumbnail(
    imageReference: String,
    contentDescription: String?,
    modifier: Modifier,
    contentScale: ContentScale,
) {
    val painter =
        when (imageReference) {
            ROOF_GARDEN_REFERENCE -> painterResource(Res.drawable.demo_roof_garden)
            REPAIR_CAFE_REFERENCE -> painterResource(Res.drawable.demo_repair_cafe)
            else -> ColorPainter(MaterialTheme.colorScheme.surfaceContainer)
        }
    Image(
        painter = painter,
        contentDescription = contentDescription,
        contentScale = contentScale,
        modifier = modifier,
    )
}
