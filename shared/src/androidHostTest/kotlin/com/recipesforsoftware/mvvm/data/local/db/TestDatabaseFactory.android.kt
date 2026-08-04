package com.recipesforsoftware.mvvm.data.local.db

import android.content.Context
import androidx.room.Room
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import java.io.File
import java.util.concurrent.Executor

/**
 * Android host-test factory: builds a Room database on a temporary file using a
 * mocked [Context] and a JDBC SQLite driver. The bundled AndroidX SQLite driver
 * loads native libraries that are unavailable on a desktop JVM, so host tests
 * use this JVM-compatible driver instead.
 */
actual fun createTestDatabase(): SignalBriefDatabase {
    val databaseFile = File.createTempFile("signalbrief-test", ".db").apply { deleteOnExit() }
    val context =
        mockk<Context>(relaxed = true) {
            every { getDatabasePath(any()) } returns databaseFile
            every { mainExecutor } returns Executor { it.run() }
        }

    return Room
        .databaseBuilder<SignalBriefDatabase>(
            context = context,
            name = databaseFile.absolutePath,
            factory = { SignalBriefDatabaseConstructor.initialize() },
        ).setDriver(JdbcSqliteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .build()
}
