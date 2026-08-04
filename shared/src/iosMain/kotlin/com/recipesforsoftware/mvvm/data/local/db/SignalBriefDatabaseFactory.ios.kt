package com.recipesforsoftware.mvvm.data.local.db

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.recipesforsoftware.mvvm.data.local.DATABASE_FILE_NAME
import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSApplicationSupportDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/**
 * Creates the [SignalBriefDatabase] for iOS.
 *
 * The database is placed in the Application Support directory inside a
 * `databases` subdirectory. The directory is created on first use. No personal
 * absolute path is hard-coded.
 */
@OptIn(ExperimentalForeignApi::class)
fun createSignalBriefDatabase(): SignalBriefDatabase {
    val fileManager = NSFileManager.defaultManager
    val directoryUrl =
        fileManager.URLForDirectory(
            directory = NSApplicationSupportDirectory,
            inDomain = NSUserDomainMask,
            appropriateForURL = null,
            create = true,
            error = null,
        )
    requireNotNull(directoryUrl) { "Could not resolve Application Support directory" }

    val databaseDirectory = "${directoryUrl.path!!}/databases"
    if (!fileManager.fileExistsAtPath(databaseDirectory)) {
        fileManager.createDirectoryAtPath(
            databaseDirectory,
            withIntermediateDirectories = true,
            attributes = null,
            error = null,
        )
    }

    return Room
        .databaseBuilder<SignalBriefDatabase>(
            name = "$databaseDirectory/$DATABASE_FILE_NAME",
            factory = { SignalBriefDatabaseConstructor.initialize() },
        ).setDriver(BundledSQLiteDriver())
        .build()
}
