package pl.recipesforsoftware.signalbrief.data.local.db

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import org.junit.Test
import java.io.File
import java.sql.DriverManager

/**
 * Regression test for the v1 → v2 database migration.
 *
 * Creates a v1 database fixture using raw JDBC (historical schema), runs the
 * production [MIGRATION_1_2] via an [SQLiteConnection] adapter backed by JDBC,
 * and verifies that cached data survives and the saved_articles table is created
 * correctly.
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

    /**
     * Executes the production [MIGRATION_1_2] against the v1 database file.
     *
     * Opens the file via JDBC and wraps the connection in a minimal
     * [SQLiteConnection] adapter so that [Migration.migrate] receives the
     * exact interface it expects.
     */
    private fun runProductionMigration1to2(file: File) {
        val jdbcConnection = DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}")
        val connection = JdbcSqliteConnectionAdapter(jdbcConnection)
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
                // Verify cached data survived migration.
                val cached = stmt.executeQuery("SELECT url, title FROM cached_articles")
                assert(cached.next()) { "Cached article lost after migration" }
                assert(cached.getString("url") == "https://example.com/existing") {
                    "Cached URL mismatch: ${cached.getString("url")}"
                }
                assert(cached.getString("title") == "Existing Article") {
                    "Cached title mismatch: ${cached.getString("title")}"
                }
                assert(!cached.next()) { "Unexpected extra cached rows" }

                // Verify saved_articles table exists and is empty.
                val saved = stmt.executeQuery("SELECT COUNT(*) AS cnt FROM saved_articles")
                assert(saved.next())
                assert(saved.getInt("cnt") == 0) {
                    "Expected empty saved_articles after migration, got ${saved.getInt("cnt")}"
                }

                // Verify saved_articles schema by inserting and reading a row.
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

                // Verify the index exists.
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

/**
 * Minimal [SQLiteConnection] adapter backed by a JDBC connection.
 *
 * Only implements the subset required by Room migrations ([prepare] and
 * [close]). All bind/get accessors throw [UnsupportedOperationException]
 * because migrations never read or bind parameters.
 */
private class JdbcSqliteConnectionAdapter(
    private val jdbcConnection: java.sql.Connection,
) : SQLiteConnection {
    override fun prepare(sql: String): SQLiteStatement {
        val jdbcStmt = jdbcConnection.prepareStatement(sql)
        return JdbcSqliteStatementAdapter(jdbcStmt)
    }

    override fun inTransaction(): Boolean = false

    override fun close() {
        jdbcConnection.close()
    }
}

/**
 * Minimal [SQLiteStatement] adapter backed by a JDBC
 * [java.sql.PreparedStatement].
 *
 * [step] executes the statement and returns `false` for DDL/DML (no result
 * rows) or `true` when a [ResultSet][java.sql.ResultSet] is available. All
 * bind/get accessors are stubs – migrations only call [step] and [close].
 */
@Suppress("TooManyFunctions")
private class JdbcSqliteStatementAdapter(
    private val jdbcStmt: java.sql.PreparedStatement,
) : SQLiteStatement {
    override fun step(): Boolean {
        val hasResultSet = jdbcStmt.execute()
        return if (hasResultSet) {
            val rs = jdbcStmt.resultSet
            rs != null && rs.next()
        } else {
            false
        }
    }

    override fun close() {
        jdbcStmt.close()
    }

    override fun bindBlob(
        index: Int,
        value: ByteArray,
    ) = throw UnsupportedOperationException()

    override fun bindDouble(
        index: Int,
        value: Double,
    ) = throw UnsupportedOperationException()

    override fun bindLong(
        index: Int,
        value: Long,
    ) = throw UnsupportedOperationException()

    override fun bindText(
        index: Int,
        value: String,
    ) = throw UnsupportedOperationException()

    override fun bindNull(index: Int) = throw UnsupportedOperationException()

    override fun getBlob(index: Int): ByteArray = throw UnsupportedOperationException()

    override fun getDouble(index: Int): Double = throw UnsupportedOperationException()

    override fun getLong(index: Int): Long = throw UnsupportedOperationException()

    override fun getText(index: Int): String = throw UnsupportedOperationException()

    override fun isNull(index: Int): Boolean = throw UnsupportedOperationException()

    override fun getColumnCount(): Int = throw UnsupportedOperationException()

    override fun getColumnName(index: Int): String = throw UnsupportedOperationException()

    override fun getColumnType(index: Int): Int = throw UnsupportedOperationException()

    override fun reset() = throw UnsupportedOperationException()

    override fun clearBindings() = throw UnsupportedOperationException()
}
