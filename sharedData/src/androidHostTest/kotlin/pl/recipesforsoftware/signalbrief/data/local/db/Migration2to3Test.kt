package pl.recipesforsoftware.signalbrief.data.local.db

import org.junit.Test
import java.io.File
import java.sql.DriverManager

/**
 * Regression test for the v2 → v3 database migration.
 *
 * Creates a v2 database fixture using raw JDBC (historical schema), runs the
 * production [MIGRATION_2_3] via an [SQLiteConnection][androidx.sqlite.SQLiteConnection]
 * adapter backed by JDBC, and verifies that cached and saved data survive and
 * the collections table is created correctly.
 */
class Migration2to3Test {
    @Test
    fun migrate2to3CachedAndSavedDataSurvivesAndCollectionsTableIsCreated() {
        val databaseFile = File.createTempFile("migration-test-2to3", ".db").apply { deleteOnExit() }

        createV2DatabaseWithData(databaseFile)
        runProductionMigration2to3(databaseFile)
        verifyMigratedDatabase(databaseFile)
    }

    private fun createV2DatabaseWithData(file: File) {
        val connection = DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}")
        connection.use { conn ->
            conn.createStatement().use { stmt ->
                V2_CREATE_CACHED_ARTICLES.forEach(stmt::executeUpdate)
                V2_CREATE_SAVED_ARTICLES.forEach(stmt::executeUpdate)
                stmt.executeUpdate(ROOM_MASTER_DDL)
                stmt.executeUpdate(
                    "INSERT OR REPLACE INTO room_master_table (id, identity_hash)" +
                        " VALUES(42, '$V2_IDENTITY_HASH')",
                )
                V2_SAMPLE_DATA.forEach(stmt::executeUpdate)
            }
        }
    }

    private fun runProductionMigration2to3(file: File) {
        val jdbcConnection = DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}")
        val connection = JdbcMigrationConnectionAdapter(jdbcConnection)
        try {
            MIGRATION_2_3.migrate(connection)
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
                assert(cached.getString("url") == CACHED_URL) { "Cached URL mismatch" }
                assert(cached.getString("title") == CACHED_TITLE) { "Cached title mismatch" }
                assert(!cached.next()) { "Unexpected extra cached rows" }

                val saved = stmt.executeQuery("SELECT url, title FROM saved_articles")
                assert(saved.next()) { "Saved article lost after migration" }
                assert(saved.getString("url") == SAVED_URL) { "Saved URL mismatch" }
                assert(saved.getString("title") == SAVED_TITLE) { "Saved title mismatch" }
                assert(!saved.next()) { "Unexpected extra saved rows" }

                val count = stmt.executeQuery("SELECT COUNT(*) AS cnt FROM collections")
                assert(count.next())
                assert(count.getInt("cnt") == 0) { "Expected empty collections after migration" }

                stmt.executeUpdate(
                    "INSERT INTO collections (name, created_at) VALUES ('Reading', $EXPECTED_CREATED_AT)",
                )
                val inserted =
                    stmt.executeQuery(
                        "SELECT id, name, created_at FROM collections WHERE name = 'Reading'",
                    )
                assert(inserted.next()) { "Collection not found after insert" }
                assert(inserted.getLong("id") > MIN_VALID_ID) { "Auto-generated id should be positive" }
                assert(inserted.getString("name") == "Reading") { "Name mismatch" }
                assert(inserted.getLong("created_at") == EXPECTED_CREATED_AT) { "CreatedAt mismatch" }
                assert(!inserted.next()) { "Unexpected extra collection rows" }

                val indexes =
                    stmt.executeQuery(
                        "SELECT name FROM sqlite_master" +
                            " WHERE type = 'index' AND name = 'index_collections_created_at'",
                    )
                assert(indexes.next()) { "created_at index not found after migration" }
            }
        }
    }

    private companion object {
        const val V2_IDENTITY_HASH = "1dda1bd8d924cd221a2c046921697738"
        const val MIN_VALID_ID = 0L
        const val EXPECTED_CREATED_AT = 9000L
        const val CACHED_URL = "https://example.com/cached"
        const val CACHED_TITLE = "Cached Article"
        const val SAVED_URL = "https://example.com/saved"
        const val SAVED_TITLE = "Saved Article"

        const val ROOM_MASTER_DDL = """
            CREATE TABLE IF NOT EXISTS room_master_table (
                id INTEGER PRIMARY KEY,
                identity_hash TEXT
            )
        """

        val V2_CREATE_CACHED_ARTICLES =
            listOf(
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
                "CREATE INDEX IF NOT EXISTS `index_cached_articles_country_feed_position_in_feed`" +
                    " ON `cached_articles` (`country`, `feed`, `position_in_feed`)",
            )

        val V2_CREATE_SAVED_ARTICLES =
            listOf(
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
                "CREATE INDEX IF NOT EXISTS `index_saved_articles_saved_at`" +
                    " ON `saved_articles` (`saved_at`)",
            )

        val V2_SAMPLE_DATA =
            listOf(
                """
                INSERT INTO cached_articles
                    (country, feed, url, title, description, image_url, source_id, source_name, position_in_feed)
                VALUES
                    ('us', 'top-headlines', '$CACHED_URL', '$CACHED_TITLE', null, null, null, null, 0)
                """,
                """
                INSERT INTO saved_articles
                    (url, title, description, image_url, source_id, source_name, saved_at)
                VALUES
                    ('$SAVED_URL', '$SAVED_TITLE', 'Description', null, null, null, 5000)
                """,
            )
    }
}
