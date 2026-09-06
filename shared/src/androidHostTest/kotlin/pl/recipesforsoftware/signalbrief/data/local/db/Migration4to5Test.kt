package pl.recipesforsoftware.signalbrief.data.local.db

import org.junit.Test
import java.io.File
import java.sql.DriverManager
import java.sql.Statement

/**
 * Regression test for the v4 → v5 database migration.
 *
 * Creates a v4 database fixture using raw JDBC (historical schema), runs the
 * production [MIGRATION_4_5] via an [SQLiteConnection][androidx.sqlite.SQLiteConnection]
 * adapter backed by JDBC, and verifies that existing cached, saved, collection,
 * and membership data survives and the monitored_topics table (with its unique
 * normalized-query index) is created correctly.
 */
class Migration4to5Test {
    @Test
    fun migrate4to5PreservesExistingDataAndEnablesMonitoredTopics() {
        val databaseFile = File.createTempFile("migration-test-4to5", ".db").apply { deleteOnExit() }
        createV4Database(databaseFile)
        runMigration(databaseFile)

        DriverManager.getConnection("jdbc:sqlite:${databaseFile.absolutePath}").use { connection ->
            val statement = connection.createStatement()
            assertExistingDataSurvives(statement)
            assertMonitoredTopicsTableWorks(statement)
            assertUniqueIndexEnforced(statement)
        }
    }

    private fun assertExistingDataSurvives(statement: Statement) {
        assert(
            statement
                .executeQuery("SELECT url FROM cached_articles")
                .let { it.next() && it.getString(1) == CACHED_URL },
        )
        assert(
            statement
                .executeQuery("SELECT url FROM saved_articles")
                .let { it.next() && it.getString(1) == SAVED_URL },
        )
        assert(
            statement
                .executeQuery("SELECT name FROM collections")
                .let { it.next() && it.getString(1) == "Reading" },
        )
        assert(
            statement
                .executeQuery("SELECT article_id FROM article_collection_memberships")
                .let { it.next() && it.getString(1) == SAVED_URL },
        )
    }

    private fun assertMonitoredTopicsTableWorks(statement: Statement) {
        statement.executeUpdate(
            "INSERT INTO monitored_topics (query, normalized_query)" +
                " VALUES ('Kotlin', 'kotlin')",
        )
        statement.executeQuery("SELECT id, query, normalized_query FROM monitored_topics").use { rows ->
            assert(rows.next())
            assert(rows.getLong("id") > 0L)
            assert(rows.getString("query") == "Kotlin")
            assert(rows.getString("normalized_query") == "kotlin")
            assert(!rows.next()) { "Unexpected extra monitored topic rows" }
        }
    }

    private fun assertUniqueIndexEnforced(statement: Statement) {
        statement
            .executeQuery(
                "SELECT name FROM sqlite_master" +
                    " WHERE type = 'index' AND name = 'index_monitored_topics_normalized_query'",
            ).use { rows ->
                assert(rows.next()) { "normalized_query index not found after migration" }
            }
        val duplicateFailed =
            try {
                statement.executeUpdate(
                    "INSERT INTO monitored_topics (query, normalized_query)" +
                        " VALUES ('kotlin', 'kotlin')",
                )
                false
            } catch (expected: Exception) {
                true
            }
        assert(duplicateFailed) { "Duplicate normalized_query should violate the unique index" }
    }

    private fun createV4Database(file: File) {
        DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use { connection ->
            val statement = connection.createStatement()
            createArticleTables(statement)
            createCollectionTables(statement)
            createRoomMasterTable(statement)
            insertSampleData(statement)
        }
    }

    private fun createArticleTables(statement: Statement) {
        statement.executeUpdate(
            """
            CREATE TABLE cached_articles (
                country TEXT NOT NULL,
                feed TEXT NOT NULL,
                url TEXT NOT NULL,
                title TEXT,
                description TEXT,
                image_url TEXT,
                source_id TEXT,
                source_name TEXT,
                position_in_feed INTEGER NOT NULL,
                PRIMARY KEY(country, feed, url)
            )
            """,
        )
        statement.executeUpdate(
            """
            CREATE TABLE saved_articles (
                url TEXT NOT NULL PRIMARY KEY,
                title TEXT,
                description TEXT,
                image_url TEXT,
                source_id TEXT,
                source_name TEXT,
                saved_at INTEGER NOT NULL
            )
            """,
        )
    }

    private fun createCollectionTables(statement: Statement) {
        statement.executeUpdate(
            """
            CREATE TABLE collections (
                id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                name TEXT NOT NULL,
                created_at INTEGER NOT NULL
            )
            """,
        )
        statement.executeUpdate(
            """
            CREATE TABLE article_collection_memberships (
                collection_id INTEGER NOT NULL,
                article_id TEXT NOT NULL,
                title TEXT,
                description TEXT,
                image_url TEXT,
                source_id TEXT,
                source_name TEXT,
                PRIMARY KEY(collection_id, article_id),
                FOREIGN KEY(collection_id) REFERENCES collections(id)
                    ON UPDATE NO ACTION ON DELETE CASCADE
            )
            """,
        )
    }

    private fun createRoomMasterTable(statement: Statement) {
        statement.executeUpdate(
            """
            CREATE TABLE room_master_table (
                id INTEGER PRIMARY KEY,
                identity_hash TEXT
            )
            """,
        )
        statement.executeUpdate(
            "INSERT OR REPLACE INTO room_master_table (id, identity_hash)" +
                " VALUES(42, '810ceb4b45bcf94bed99d8d2cfd75d7a')",
        )
    }

    private fun insertSampleData(statement: Statement) {
        statement.executeUpdate(
            "INSERT INTO cached_articles (country, feed, url, title, position_in_feed)" +
                " VALUES ('us', 'top-headlines', '$CACHED_URL', 'Cached', 0)",
        )
        statement.executeUpdate(
            "INSERT INTO saved_articles (url, title, saved_at)" +
                " VALUES ('$SAVED_URL', 'Saved', 1000)",
        )
        statement.executeUpdate(
            "INSERT INTO collections (id, name, created_at) VALUES (1, 'Reading', 1000)",
        )
        statement.executeUpdate(
            "INSERT INTO article_collection_memberships (collection_id, article_id, title)" +
                " VALUES (1, '$SAVED_URL', 'Saved')",
        )
    }

    private fun runMigration(file: File) {
        val connection = JdbcMigrationConnectionAdapter(DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}"))
        try {
            MIGRATION_4_5.migrate(connection)
        } finally {
            connection.close()
        }
    }

    private companion object {
        const val CACHED_URL = "https://example.com/cached"
        const val SAVED_URL = "https://example.com/saved"
    }
}
