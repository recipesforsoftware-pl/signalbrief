package pl.recipesforsoftware.signalbrief.data.local.db

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteStatement
import java.sql.Connection

/**
 * Minimal [SQLiteConnection] adapter backed by a JDBC connection.
 *
 * Only implements the subset required by Room migrations ([prepare] and
 * [close]). All bind/get accessors throw [UnsupportedOperationException]
 * because migrations never read or bind parameters.
 */
internal class JdbcMigrationConnectionAdapter(
    private val jdbcConnection: Connection,
) : SQLiteConnection {
    override fun prepare(sql: String): SQLiteStatement {
        val jdbcStmt = jdbcConnection.prepareStatement(sql)
        return JdbcMigrationStatementAdapter(jdbcStmt)
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
internal class JdbcMigrationStatementAdapter(
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
