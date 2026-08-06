package pl.recipesforsoftware.signalbrief.ui.designsystem.components

import org.jetbrains.compose.resources.DrawableResource
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.Res
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.sigby_compact
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.sigby_full

/**
 * Pure selection logic that maps a [SigbyVariant] to its Compose Multiplatform
 * drawable resource. Kept separate from the composable so it can be unit-tested
 * without instantiating Compose UI.
 */
internal object SigbyResourceResolver {
    fun resolve(variant: SigbyVariant): DrawableResource =
        when (variant) {
            SigbyVariant.Full -> Res.drawable.sigby_full
            SigbyVariant.Compact -> Res.drawable.sigby_compact
        }
}
