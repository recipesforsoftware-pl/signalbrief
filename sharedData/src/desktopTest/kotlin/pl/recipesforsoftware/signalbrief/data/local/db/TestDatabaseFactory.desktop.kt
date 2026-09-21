package pl.recipesforsoftware.signalbrief.data.local.db

import java.io.File

/**
 * JVM Desktop test factory: builds a Room database on a fresh temporary file for
 * every call. The file is unique per test and removed when the process exits, so
 * tests stay isolated on both macOS and Windows without a hard-coded path.
 */
actual fun createTestDatabase(): SignalBriefDatabase {
    val databaseFile =
        File.createTempFile("signalbrief-test", ".db").apply {
            delete()
            deleteOnExit()
        }
    return createSignalBriefDatabase(databaseFile.absolutePath)
}
