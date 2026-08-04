package com.recipesforsoftware.mvvm.data.local.db

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID

/**
 * iOS test factory: builds a Room database on a temporary file. The file is
 * unique per test run and is dropped automatically when the process exits.
 */
actual fun createTestDatabase(): SignalBriefDatabase {
    val databasePath = "${NSTemporaryDirectory()}/signalbrief-test-${NSUUID.UUID().UUIDString}.db"
    return Room
        .databaseBuilder<SignalBriefDatabase>(
            name = databasePath,
            factory = { SignalBriefDatabaseConstructor.initialize() },
        ).setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.Default)
        .build()
}
