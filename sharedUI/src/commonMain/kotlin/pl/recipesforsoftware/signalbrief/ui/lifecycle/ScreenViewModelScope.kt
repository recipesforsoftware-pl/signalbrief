package pl.recipesforsoftware.signalbrief.ui.lifecycle

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner

/**
 * Provides a ViewModel store whose lifetime is limited to this composable
 * subtree.
 *
 * Use this at a state-driven screen boundary when the host does not supply a
 * navigation back-stack entry. Leaving the subtree clears every ViewModel in
 * the store and cancels its [androidx.lifecycle.viewModelScope] work.
 */
@Composable
fun ScreenViewModelScope(content: @Composable () -> Unit) {
    val owner = remember { ScreenViewModelStoreOwner() }

    DisposableEffect(owner) {
        onDispose { owner.viewModelStore.clear() }
    }

    CompositionLocalProvider(LocalViewModelStoreOwner provides owner) {
        content()
    }
}

private class ScreenViewModelStoreOwner : ViewModelStoreOwner {
    override val viewModelStore = ViewModelStore()
}
