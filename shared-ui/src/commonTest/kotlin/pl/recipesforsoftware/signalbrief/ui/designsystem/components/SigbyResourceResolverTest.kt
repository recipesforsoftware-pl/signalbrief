package pl.recipesforsoftware.signalbrief.ui.designsystem.components

import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.Res
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.sigby_compact
import pl.recipesforsoftware.signalbrief.sharedui.generated.resources.sigby_full
import kotlin.test.Test
import kotlin.test.assertEquals

class SigbyResourceResolverTest {
    @Test
    fun `Full variant resolves to sigby_full drawable`() {
        assertEquals(Res.drawable.sigby_full, SigbyResourceResolver.resolve(SigbyVariant.Full))
    }

    @Test
    fun `Compact variant resolves to sigby_compact drawable`() {
        assertEquals(Res.drawable.sigby_compact, SigbyResourceResolver.resolve(SigbyVariant.Compact))
    }
}
