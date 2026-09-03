package pl.recipesforsoftware.signalbrief.data.local.db

import androidx.room.ConstructedBy
import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.SQLiteConnection
import androidx.sqlite.execSQL
import pl.recipesforsoftware.signalbrief.data.local.dao.CachedArticleDao
import pl.recipesforsoftware.signalbrief.data.local.dao.CollectionDao
import pl.recipesforsoftware.signalbrief.data.local.dao.SavedArticleDao
import pl.recipesforsoftware.signalbrief.data.local.entity.CachedArticleEntity
import pl.recipesforsoftware.signalbrief.data.local.entity.CollectionEntity
import pl.recipesforsoftware.signalbrief.data.local.entity.SavedArticleEntity

/**
 * Room database for the shared data layer.
 *
 * Version 3 adds the collections table. Schema export is enabled so
 * migrations can be validated against checked-in JSON files.
 */
@Database(
    entities = [CachedArticleEntity::class, SavedArticleEntity::class, CollectionEntity::class],
    version = 3,
    exportSchema = true,
)
@ConstructedBy(SignalBriefDatabaseConstructor::class)
abstract class SignalBriefDatabase : RoomDatabase() {
    internal abstract fun articleDao(): CachedArticleDao

    internal abstract fun savedArticleDao(): SavedArticleDao

    internal abstract fun collectionDao(): CollectionDao
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

/**
 * Version 2 → 3: adds the `collections` table.
 *
 * This is a purely additive migration. The existing `cached_articles` and
 * `saved_articles` tables are untouched and all cached/saved data is preserved.
 */
val MIGRATION_2_3 =
    object : Migration(2, 3) {
        override fun migrate(connection: SQLiteConnection) {
            connection.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `collections` (
                    `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                    `name` TEXT NOT NULL,
                    `created_at` INTEGER NOT NULL
                )
                """,
            )
            connection.execSQL(
                "CREATE INDEX IF NOT EXISTS `index_collections_created_at`" +
                    " ON `collections` (`created_at`)",
            )
        }
    }
