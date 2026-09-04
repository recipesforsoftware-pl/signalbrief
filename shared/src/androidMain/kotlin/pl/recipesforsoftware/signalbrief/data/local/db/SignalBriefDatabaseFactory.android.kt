package pl.recipesforsoftware.signalbrief.data.local.db

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.coroutines.Dispatchers
import pl.recipesforsoftware.signalbrief.data.local.DATABASE_FILE_NAME

/**
 * Creates the [SignalBriefDatabase] for Android.
 *
 * The supplied [context] is switched to its application context, main-thread
 * queries are disallowed, and the bundled SQLite driver is used. The explicit
 * additive migration ensures existing cached headline data is preserved.
 */
fun createSignalBriefDatabase(context: Context): SignalBriefDatabase {
    val applicationContext = context.applicationContext
    return Room
        .databaseBuilder<SignalBriefDatabase>(
            context = applicationContext,
            name = applicationContext.getDatabasePath(DATABASE_FILE_NAME).absolutePath,
            factory = { SignalBriefDatabaseConstructor.initialize() },
        ).setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.IO)
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
        .build()
}
