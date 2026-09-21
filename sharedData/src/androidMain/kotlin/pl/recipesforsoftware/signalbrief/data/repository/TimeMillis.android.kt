package pl.recipesforsoftware.signalbrief.data.repository

/**
 * Android epoch-millisecond timestamp backed by [System.currentTimeMillis].
 */
internal actual fun currentTimeMillis(): Long = System.currentTimeMillis()
