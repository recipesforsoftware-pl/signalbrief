package pl.recipesforsoftware.signalbrief.data.local.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import pl.recipesforsoftware.signalbrief.data.local.dao.CachedArticleDao
import pl.recipesforsoftware.signalbrief.data.local.entity.CachedArticleEntity

/**
 * Room database for the shared data layer.
 *
 * Version 1 contains only the cached top-headlines table. Schema export is
 * enabled so migrations can be validated against checked-in JSON files.
 */
@Database(
    entities = [CachedArticleEntity::class],
    version = 1,
    exportSchema = true,
)
@ConstructedBy(SignalBriefDatabaseConstructor::class)
abstract class SignalBriefDatabase : RoomDatabase() {
    internal abstract fun articleDao(): CachedArticleDao
}
