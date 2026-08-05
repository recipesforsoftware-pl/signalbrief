package com.recipesforsoftware.mvvm.ui.onboarding

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

// Geometric constants for the editorial newspaper motif.
private const val ASPECT_RATIO = 1.2f
private const val PAGE_WIDTH_RATIO = 0.55f
private const val PAGE_HEIGHT_RATIO = 0.75f
private const val PAGE_ROTATION = -6f
private const val BACKDROP_ROTATION = 8f
private const val BACKDROP_ALPHA = 0.6f
private const val CORNER_DP = 12
private const val MASTHEAD_LEFT_RATIO = 0.08f
private const val MASTHEAD_TOP_RATIO = 0.12f
private const val MASTHEAD_WIDTH_RATIO = 0.84f
private const val MASTHEAD_HEIGHT_RATIO = 0.08f
private const val LINE_LEFT_RATIO = 0.08f
private const val LINE_WIDTH_RATIO = 0.84f
private const val LINE_START_RATIO = 0.28f
private const val LINE_GAP_RATIO = 0.07f
private const val LINE_THICKNESS_RATIO = 0.018f
private const val FIRST_LINE_WIDTH_RATIO = 1.0f
private const val LAST_LINE_WIDTH_RATIO = 0.55f
private const val DEFAULT_LINE_WIDTH_RATIO = 0.75f
private const val LINE_COUNT = 5
private const val BACKDROP_LEFT_RATIO = 0.35f
private const val BACKDROP_TOP_RATIO = 0.2f
private const val BACKDROP_OFFSET_X_RATIO = 1.15f
private const val BACKDROP_OFFSET_Y_RATIO = 0.85f

/**
 * Lightweight vector-style onboarding visual with an editorial/news motif.
 *
 * Draws a stylised newspaper page made of simple rectangles and lines. It
 * contains no copyrighted artwork and adapts automatically to light and dark
 * mode through the current Material 3 color scheme.
 */
@Composable
fun OnboardingVisual(
    modifier: Modifier = Modifier,
    contentDescription: String? = OnboardingStrings.VISUAL_DECORATIVE,
) {
    val semanticModifier =
        if (contentDescription != null) {
            Modifier.semantics { this.contentDescription = contentDescription }
        } else {
            Modifier
        }

    val pageColor = MaterialTheme.colorScheme.surfaceVariant
    val inkColor = MaterialTheme.colorScheme.onSurfaceVariant
    val accentColor = MaterialTheme.colorScheme.primary

    Canvas(
        modifier =
            modifier
                .fillMaxWidth()
                .aspectRatio(ASPECT_RATIO)
                .then(semanticModifier),
    ) {
        val pageWidth = size.width * PAGE_WIDTH_RATIO
        val pageHeight = size.height * PAGE_HEIGHT_RATIO
        val left = (size.width - pageWidth) / 2f
        val top = (size.height - pageHeight) / 2f
        val cornerRadius = CornerRadius(CORNER_DP.dp.toPx(), CORNER_DP.dp.toPx())

        drawFrontPage(
            pageColor = pageColor,
            inkColor = inkColor,
            accentColor = accentColor,
            pageWidth = pageWidth,
            pageHeight = pageHeight,
            left = left,
            top = top,
            cornerRadius = cornerRadius,
        )

        drawBackdropPage(
            pageColor = pageColor,
            pageWidth = pageWidth,
            pageHeight = pageHeight,
            left = left,
            top = top,
            cornerRadius = cornerRadius,
        )
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawFrontPage(
    pageColor: androidx.compose.ui.graphics.Color,
    inkColor: androidx.compose.ui.graphics.Color,
    accentColor: androidx.compose.ui.graphics.Color,
    pageWidth: Float,
    pageHeight: Float,
    left: Float,
    top: Float,
    cornerRadius: CornerRadius,
) {
    rotate(degrees = PAGE_ROTATION, pivot = Offset(left + pageWidth / 2, top + pageHeight / 2)) {
        // Page background
        drawRoundRect(
            color = pageColor,
            topLeft = Offset(left, top),
            size = Size(pageWidth, pageHeight),
            cornerRadius = cornerRadius,
        )

        // Masthead bar
        drawRect(
            color = accentColor,
            topLeft = Offset(left + pageWidth * MASTHEAD_LEFT_RATIO, top + pageHeight * MASTHEAD_TOP_RATIO),
            size = Size(pageWidth * MASTHEAD_WIDTH_RATIO, pageHeight * MASTHEAD_HEIGHT_RATIO),
        )

        // Headline lines
        val lineWidth = pageWidth * LINE_WIDTH_RATIO
        val lineLeft = left + pageWidth * LINE_LEFT_RATIO
        val lineStartY = top + pageHeight * LINE_START_RATIO
        val lineGap = pageHeight * LINE_GAP_RATIO
        val lineThickness = pageHeight * LINE_THICKNESS_RATIO

        repeat(LINE_COUNT) { index ->
            val y = lineStartY + index * lineGap
            val widthFactor =
                when (index) {
                    0 -> FIRST_LINE_WIDTH_RATIO
                    LINE_COUNT - 1 -> LAST_LINE_WIDTH_RATIO
                    else -> DEFAULT_LINE_WIDTH_RATIO
                }
            drawRect(
                color = inkColor,
                topLeft = Offset(lineLeft, y),
                size = Size(lineWidth * widthFactor, lineThickness),
            )
        }
    }
}

private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawBackdropPage(
    pageColor: androidx.compose.ui.graphics.Color,
    pageWidth: Float,
    pageHeight: Float,
    left: Float,
    top: Float,
    cornerRadius: CornerRadius,
) {
    rotate(
        degrees = BACKDROP_ROTATION,
        pivot = Offset(left + pageWidth * BACKDROP_OFFSET_X_RATIO, top + pageHeight * BACKDROP_OFFSET_Y_RATIO),
    ) {
        drawRoundRect(
            color = pageColor.copy(alpha = BACKDROP_ALPHA),
            topLeft = Offset(left + pageWidth * BACKDROP_LEFT_RATIO, top + pageHeight * BACKDROP_TOP_RATIO),
            size = Size(pageWidth, pageHeight),
            cornerRadius = cornerRadius,
        )
    }
}
