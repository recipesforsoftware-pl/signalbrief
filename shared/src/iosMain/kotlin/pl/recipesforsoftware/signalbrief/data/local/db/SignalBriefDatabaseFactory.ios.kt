package pl.recipesforsoftware.signalbrief.data.local.db

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.Dispatchers
import pl.recipesforsoftware.signalbrief.data.local.DATABASE_FILE_NAME
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSError
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/**
 * Creates the [SignalBriefDatabase] for iOS.
 *
 * The database is placed in the Application Support directory inside a
 * `databases` subdirectory. The directory is created on first use. No personal
 * absolute path is hard-coded.
 */
@OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)
fun createSignalBriefDatabase(): SignalBriefDatabase {
    val fileManager = NSFileManager.defaultManager
    val directoryUrl =
        memScoped {
            val error = alloc<ObjCObjectVar<NSError?>>()
            fileManager.URLForDirectory(
                directory = NSApplicationSupportDirectory,
                inDomain = NSUserDomainMask,
                appropriateForURL = null,
                create = true,
                error = error.ptr,
            )
        }
    requireNotNull(directoryUrl) { "Could not resolve Application Support directory" }

    val applicationSupportPath = directoryUrl.path
    requireNotNull(applicationSupportPath) { "Could not resolve Application Support path" }

    val databaseDirectory = "$applicationSupportPath/databases"
    if (!fileManager.fileExistsAtPath(databaseDirectory)) {
        memScoped {
            val error = alloc<ObjCObjectVar<NSError?>>()
            val created =
                fileManager.createDirectoryAtPath(
                    databaseDirectory,
                    withIntermediateDirectories = true,
                    attributes = null,
                    error = error.ptr,
                )
            check(created) {
                "Could not create database directory: " +
                    "${error.value?.localizedDescription ?: "unknown error"}"
            }
        }
    }
    check(fileManager.fileExistsAtPath(databaseDirectory)) {
        "Database directory does not exist after creation: $databaseDirectory"
    }

    return Room
        .databaseBuilder<SignalBriefDatabase>(
            name = "$databaseDirectory/$DATABASE_FILE_NAME",
            factory = { SignalBriefDatabaseConstructor.initialize() },
        ).setDriver(BundledSQLiteDriver())
        .setQueryCoroutineContext(Dispatchers.Default)
        .addMigrations(MIGRATION_1_2, MIGRATION_2_3)
        .build()
}
