package pl.recipesforsoftware.signalbrief.data.local.db

import org.junit.Test
import java.io.File
import java.sql.DriverManager

/**
 * Regression test for the v1 → v2 database migration.
 *
 * Creates a v1 database fixture using raw JDBC (historical schema), runs the
 * production [MIGRATION_1_2] via an [SQLiteConnection][androidx.sqlite.SQLiteConnection]
 * adapter backed by JDBC, and verifies that cached data survives and the
 * saved_articles table is created correctly.
 */
class Migration1to2Test {
    @Test
    fun migrate1to2CachedDataSurvivesAndSavedArticlesTableIsCreated() {
        val databaseFile = File.createTempFile("migration-test", ".db").apply { deleteOnExit() }

        createV1DatabaseWithCachedData(databaseFile)
        runProductionMigration1to2(databaseFile)
        verifyMigratedDatabase(databaseFile)
    }

    private fun createV1DatabaseWithCachedData(file: File) {
        val connection = DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}")
        connection.use { conn ->
            conn.createStatement().use { stmt ->
                stmt.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS `cached_articles` (
                        `country` TEXT NOT NULL,
                        `feed` TEXT NOT NULL,
                        `url` TEXT NOT NULL,
                        `title` TEXT,
                        `description` TEXT,
                        `image_url` TEXT,
                        `source_id` TEXT,
                        `source_name` TEXT,
                        `position_in_feed` INTEGER NOT NULL,
                        PRIMARY KEY(`country`, `feed`, `url`)
                    )
                    """,
                )
                stmt.executeUpdate(
                    "CREATE INDEX IF NOT EXISTS `index_cached_articles_country_feed_position_in_feed`" +
                        " ON `cached_articles` (`country`, `feed`, `position_in_feed`)",
                )
                stmt.executeUpdate(
                    """
                    CREATE TABLE IF NOT EXISTS room_master_table (
                        id INTEGER PRIMARY KEY,
                        identity_hash TEXT
                    )
                    """,
                )
                stmt.executeUpdate(
                    "INSERT OR REPLACE INTO room_master_table (id, identity_hash)" +
                        " VALUES(42, 'fd4b031326f2cbf3b731871241bd8d05')",
                )
                stmt.executeUpdate(
                    """
                    INSERT INTO cached_articles
                        (country, feed, url, title, description, image_url, source_id, source_name, position_in_feed)
                    VALUES
                        ('us', 'top-headlines', 'https://example.com/existing', 'Existing Article', null, null, null, null, 0)
                    """,
                )
            }
        }
    }

    private fun runProductionMigration1to2(file: File) {
        val jdbcConnection = DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}")
        val connection = JdbcMigrationConnectionAdapter(jdbcConnection)
        try {
            MIGRATION_1_2.migrate(connection)
        } finally {
            connection.close()
        }
    }

    private fun verifyMigratedDatabase(file: File) {
        val connection = DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}")
        connection.use { conn ->
            conn.createStatement().use { stmt ->
                val cached = stmt.executeQuery("SELECT url, title FROM cached_articles")
                assert(cached.next()) { "Cached article lost after migration" }
                assert(cached.getString("url") == "https://example.com/existing") {
                    "Cached URL mismatch: ${cached.getString("url")}"
                }
                assert(cached.getString("title") == "Existing Article") {
                    "Cached title mismatch: ${cached.getString("title")}"
                }
                assert(!cached.next()) { "Unexpected extra cached rows" }

                val saved = stmt.executeQuery("SELECT COUNT(*) AS cnt FROM saved_articles")
                assert(saved.next())
                assert(saved.getInt("cnt") == 0) {
                    "Expected empty saved_articles after migration, got ${saved.getInt("cnt")}"
                }

                stmt.executeUpdate(
                    """
                    INSERT INTO saved_articles
                        (url, title, description, image_url, source_id, source_name, saved_at)
                    VALUES
                        ('https://example.com/saved', 'Saved Title', 'Saved Desc',
                         'https://example.com/img.jpg', 'src-id', 'Source Name', 9000)
                    """,
                )

                val inserted =
                    stmt.executeQuery(
                        "SELECT url, title, description, image_url, source_id, source_name, saved_at" +
                            " FROM saved_articles WHERE url = 'https://example.com/saved'",
                    )
                assert(inserted.next()) { "Saved article not found after insert" }
                assert(inserted.getString("url") == "https://example.com/saved") { "URL mismatch" }
                assert(inserted.getString("title") == "Saved Title") { "Title mismatch" }
                assert(inserted.getString("description") == "Saved Desc") { "Description mismatch" }
                assert(inserted.getString("image_url") == "https://example.com/img.jpg") { "ImageURL mismatch" }
                assert(inserted.getString("source_id") == "src-id") { "SourceId mismatch" }
                assert(inserted.getString("source_name") == "Source Name") { "SourceName mismatch" }
                assert(inserted.getLong("saved_at") == EXPECTED_SAVED_AT) { "SavedAt mismatch" }
                assert(!inserted.next()) { "Unexpected extra saved rows" }

                val indexes =
                    stmt.executeQuery(
                        "SELECT name FROM sqlite_master" +
                            " WHERE type = 'index' AND name = 'index_saved_articles_saved_at'",
                    )
                assert(indexes.next()) { "saved_at index not found after migration" }
            }
        }
    }

    private companion object {
        const val EXPECTED_SAVED_AT = 9000L
    }
}
