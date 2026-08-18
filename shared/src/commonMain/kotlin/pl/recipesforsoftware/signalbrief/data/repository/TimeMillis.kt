package pl.recipesforsoftware.signalbrief.data.repository

/**
 * Platform-agnostic epoch-millisecond timestamp. Used by
 * [RoomSavedArticlesRepository] to stamp saved articles with a deterministic
 * ordering. The actual implementation delegates to platform APIs
 * (`System.currentTimeMillis` on Android, `NSDate` on iOS).
 */
internal expect fun currentTimeMillis(): Long
