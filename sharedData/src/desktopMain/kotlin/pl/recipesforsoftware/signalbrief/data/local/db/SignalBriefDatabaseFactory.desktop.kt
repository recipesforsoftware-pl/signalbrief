package pl.recipesforsoftware.signalbrief.data.local.db

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers

/**
 * Creates the [SignalBriefDatabase] for JVM Desktop.
 *
 * The future Desktop composition root supplies [databasePath]; this factory does
 * not resolve a macOS or Windows application-data directory. The bundled SQLite
 * driver is used and the additive migrations preserve existing user data.
 */
fun createSignalBriefDatabase(databasePath: String): SignalBriefDatabase {
    require(databasePath.isNotBlank()) { "Database path must not be blank" }

    return Room
        .databaseBuilder<SignalBriefDatabase>(
            name = databasePath,
            factory = { SignalBriefDatabaseConstructor.initialize() },
        ).setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
        .build()
}
