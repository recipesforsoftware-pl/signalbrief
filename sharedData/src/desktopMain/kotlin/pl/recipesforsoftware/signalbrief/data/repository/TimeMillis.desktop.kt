package pl.recipesforsoftware.signalbrief.data.repository

/**
 * JVM Desktop epoch-millisecond timestamp backed by [System.currentTimeMillis].
 */
internal actual fun currentTimeMillis(): Long = System.currentTimeMillis()
