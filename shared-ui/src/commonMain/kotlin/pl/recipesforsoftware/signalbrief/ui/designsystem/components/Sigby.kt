package pl.recipesforsoftware.signalbrief.ui.designsystem.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.painterResource

/**
 * Renders the SignalBrief mascot, Sigby, from the shared Compose Multiplatform
 * resource bundle.
 *
 * @param variant which artwork to render: [SigbyVariant.Full] for emotional states
 *   such as onboarding and empty states, or [SigbyVariant.Compact] for banners,
 *   errors, and smaller inline layouts.
 * @param modifier applied to the underlying [Image].
 * @param contentDescription optional localized description. Pass `null` when
 *   Sigby is purely decorative so TalkBack/VoiceOver treats the image as hidden.
 */
@Composable
fun Sigby(
    variant: SigbyVariant,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    Image(
        painter = painterResource(SigbyResourceResolver.resolve(variant)),
        contentDescription = contentDescription,
        modifier = modifier,
    )
}
