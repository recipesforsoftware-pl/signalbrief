package pl.recipesforsoftware.signalbrief.data.local.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import pl.recipesforsoftware.signalbrief.data.local.dao.CachedArticleDao
import pl.recipesforsoftware.signalbrief.data.local.dao.SavedArticleDao
import pl.recipesforsoftware.signalbrief.data.local.entity.CachedArticleEntity
import pl.recipesforsoftware.signalbrief.data.local.entity.SavedArticleEntity

/**
 * Room database for the shared data layer.
 *
 * Version 2 adds the saved-articles table. Schema export is enabled so
 * migrations can be validated against checked-in JSON files.
 */
@Database(
    entities = [CachedArticleEntity::class, SavedArticleEntity::class],
    version = 2,
    exportSchema = true,
)
@ConstructedBy(SignalBriefDatabaseConstructor::class)
abstract class SignalBriefDatabase : RoomDatabase() {
    internal abstract fun articleDao(): CachedArticleDao

    internal abstract fun savedArticleDao(): SavedArticleDao
}

/**
 * Version 1 → 2: adds the `saved_articles` table.
 *
 * This is a purely additive migration. The existing `cached_articles` table is
 * untouched and all cached data is preserved.
 */
val MIGRATION_1_2 =
    object : Migration(1, 2) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `saved_articles` (
                    `url` TEXT NOT NULL,
                    `title` TEXT,
                    `description` TEXT,
                    `image_url` TEXT,
                    `source_id` TEXT,
                    `source_name` TEXT,
                    `saved_at` INTEGER NOT NULL,
                    PRIMARY KEY(`url`)
                )
                """,
            )
            connection.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_saved_articles_saved_at`" +
                    " ON `saved_articles` (`saved_at`)",
            )
        }
    }
