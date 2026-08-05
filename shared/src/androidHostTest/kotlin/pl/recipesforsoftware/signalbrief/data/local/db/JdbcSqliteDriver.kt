package pl.recipesforsoftware.signalbrief.data.local.db

import androidx.sqlite.SQLiteConnection
import androidx.sqlite.SQLiteDriver
import androidx.sqlite.SQLiteStatement
import java.sql.Connection
import java.sql.DriverManager
import java.sql.PreparedStatement
import java.sql.ResultSet
import java.sql.Types

/**
 * Minimal JVM/Android-host SQLite driver backed by sqlite-jdbc. Used only in
 * host tests, because the bundled AndroidX SQLite driver loads native libraries
 * that are not available on a desktop JVM.
 */
class JdbcSqliteDriver : SQLiteDriver {
    override fun open(fileName: String): SQLiteConnection = JdbcSqliteConnection(fileName)
}

private class JdbcSqliteConnection(
    fileName: String,
) : SQLiteConnection {
    private val connection: Connection = DriverManager.getConnection("jdbc:sqlite:$fileName")
    private val transactionLock = Any()
    private var transactionDepth = 0

    override fun prepare(sql: String): SQLiteStatement {
        synchronized(transactionLock) {
            val normalized = sql.trim()
            when {
                normalized.startsWith("BEGIN", ignoreCase = true) -> {
                    transactionDepth++
                }

                normalized.startsWith("COMMIT", ignoreCase = true) ||
                    normalized.startsWith("END", ignoreCase = true) ||
                    normalized.startsWith("ROLLBACK", ignoreCase = true) -> {
                    transactionDepth = (transactionDepth - 1).coerceAtLeast(0)
                }
            }
        }
        return JdbcSqliteStatement(connection, sql)
    }

    override fun inTransaction(): Boolean = synchronized(transactionLock) { transactionDepth > 0 }

    override fun close() {
        connection.close()
    }
}

@Suppress("TooManyFunctions")
private class JdbcSqliteStatement(
    connection: Connection,
    sql: String,
) : SQLiteStatement {
    private val preparedStatement: PreparedStatement = connection.prepareStatement(sql)
    private var resultSet: ResultSet? = null
    private var executed = false

    override fun bindBlob(
        index: Int,
        value: ByteArray,
    ) {
        preparedStatement.setBytes(index, value)
    }

    override fun bindDouble(
        index: Int,
        value: Double,
    ) {
        preparedStatement.setDouble(index, value)
    }

    override fun bindLong(
        index: Int,
        value: Long,
    ) {
        preparedStatement.setLong(index, value)
    }

    override fun bindText(
        index: Int,
        value: String,
    ) {
        preparedStatement.setString(index, value)
    }

    override fun bindNull(index: Int) {
        preparedStatement.setNull(index, Types.NULL)
    }

    override fun step(): Boolean {
        if (executed) {
            return resultSet?.next() ?: false
        }
        executed = true
        return if (preparedStatement.execute()) {
            val rs = preparedStatement.resultSet
            resultSet = rs
            rs.next()
        } else {
            preparedStatement.resultSet?.close()
            false
        }
    }

    override fun getColumnCount(): Int = preparedStatement.metaData?.columnCount ?: 0

    override fun getColumnName(index: Int): String {
        val metaData = preparedStatement.metaData
        return metaData?.getColumnName(index.jdbcIndex()) ?: error("No result set")
    }

    override fun getColumnType(index: Int): Int {
        val columnIndex = index.jdbcIndex()
        val jdbcType = preparedStatement.metaData?.getColumnType(columnIndex) ?: return SQLITE_DATA_NULL
        return when (jdbcType) {
            Types.INTEGER, Types.BIGINT, Types.SMALLINT, Types.TINYINT -> SQLITE_DATA_INTEGER
            Types.REAL, Types.FLOAT, Types.DOUBLE, Types.NUMERIC, Types.DECIMAL -> SQLITE_DATA_FLOAT
            Types.BLOB, Types.BINARY, Types.VARBINARY -> SQLITE_DATA_BLOB
            Types.NULL -> SQLITE_DATA_NULL
            else -> SQLITE_DATA_TEXT
        }
    }

    override fun getBlob(index: Int): ByteArray = resultSet?.getBytes(index.jdbcIndex()) ?: error("No result set")

    override fun getDouble(index: Int): Double = resultSet?.getDouble(index.jdbcIndex()) ?: error("No result set")

    override fun getLong(index: Int): Long = resultSet?.getLong(index.jdbcIndex()) ?: error("No result set")

    override fun getText(index: Int): String = resultSet?.getString(index.jdbcIndex()) ?: error("No result set")

    override fun isNull(index: Int): Boolean = resultSet?.getObject(index.jdbcIndex()) == null

    override fun reset() {
        resultSet?.close()
        resultSet = null
        executed = false
        preparedStatement.clearParameters()
    }

    override fun clearBindings() {
        preparedStatement.clearParameters()
    }

    override fun close() {
        resultSet?.close()
        preparedStatement.close()
    }

    private companion object {
        const val SQLITE_DATA_INTEGER = 1
        const val SQLITE_DATA_FLOAT = 2
        const val SQLITE_DATA_TEXT = 3
        const val SQLITE_DATA_BLOB = 4
        const val SQLITE_DATA_NULL = 5
    }
}

private fun Int.jdbcIndex(): Int = this + 1
