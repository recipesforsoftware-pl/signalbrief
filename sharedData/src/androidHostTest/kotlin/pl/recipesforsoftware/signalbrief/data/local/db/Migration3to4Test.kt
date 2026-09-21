package pl.recipesforsoftware.signalbrief.data.local.db

import org.junit.Test
import java.io.File
import java.sql.DriverManager

class Migration3to4Test {
    @Test
    fun migrate3to4PreservesExistingDataAndEnablesMemberships() {
        val databaseFile = File.createTempFile("migration-test-3to4", ".db").apply { deleteOnExit() }
        createV3Database(databaseFile)
        runMigration(databaseFile)

        DriverManager.getConnection("jdbc:sqlite:${databaseFile.absolutePath}").use { connection ->
            connection.createStatement().use { statement ->
                statement.executeUpdate("PRAGMA foreign_keys = ON")
                assert(
                    statement
                        .executeQuery("SELECT name FROM collections")
                        .let { it.next() && it.getString(1) == "Reading" },
                )
                assert(
                    statement
                        .executeQuery("SELECT url FROM saved_articles")
                        .let { it.next() && it.getString(1) == ARTICLE_URL },
                )
                statement.executeUpdate(
                    "INSERT INTO article_collection_memberships " +
                        "(collection_id, article_id, title, description, image_url, source_id, source_name) " +
                        "VALUES (1, '$ARTICLE_URL', 'Headline', 'Summary', " +
                        "'https://example.com/image.png', 'source-id', 'Source Name')",
                )
                statement.executeQuery("SELECT * FROM article_collection_memberships").use { rows ->
                    assert(rows.next())
                    assert(rows.getString("article_id") == ARTICLE_URL)
                    assert(rows.getString("title") == "Headline")
                    assert(rows.getString("description") == "Summary")
                    assert(rows.getString("image_url") == "https://example.com/image.png")
                    assert(rows.getString("source_id") == "source-id")
                    assert(rows.getString("source_name") == "Source Name")
                }
                statement.executeUpdate("DELETE FROM saved_articles WHERE url = '$ARTICLE_URL'")
                statement.executeQuery("SELECT article_id, title FROM article_collection_memberships").use { rows ->
                    assert(rows.next())
                    assert(rows.getString("article_id") == ARTICLE_URL)
                    assert(rows.getString("title") == "Headline")
                }
                statement.executeUpdate("DELETE FROM collections WHERE id = 1")
                assert(
                    statement
                        .executeQuery("SELECT COUNT(*) FROM article_collection_memberships")
                        .let { it.next() && it.getInt(1) == 0 },
                )
            }
        }
    }

    private fun createV3Database(file: File) {
        DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}").use { connection ->
            connection.createStatement().use { statement ->
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
                statement.executeUpdate(
                    "INSERT INTO collections (id, name, created_at) VALUES (1, 'Reading', 1000)",
                )
                statement.executeUpdate(
                    "INSERT INTO saved_articles (url, title, saved_at) VALUES ('$ARTICLE_URL', 'Saved', 1000)",
                )
            }
        }
    }

    private fun runMigration(file: File) {
        val connection = JdbcMigrationConnectionAdapter(DriverManager.getConnection("jdbc:sqlite:${file.absolutePath}"))
        try {
            MIGRATION_3_4.migrate(connection)
        } finally {
            connection.close()
        }
    }

    private companion object {
        const val ARTICLE_URL = "https://example.com/article"
    }
}
