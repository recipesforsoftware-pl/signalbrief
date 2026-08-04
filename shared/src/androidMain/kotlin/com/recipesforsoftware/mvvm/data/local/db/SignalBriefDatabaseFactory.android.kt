package com.recipesforsoftware.mvvm.data.local.db

import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import com.recipesforsoftware.mvvm.data.local.DATABASE_FILE_NAME

/**
 * Creates the [SignalBriefDatabase] for Android.
 *
 * The supplied [context] is switched to its application context, main-thread
 * queries are disallowed, and the bundled SQLite driver is used.
 */
fun createSignalBriefDatabase(context: Context): SignalBriefDatabase {
    val applicationContext = context.applicationContext
    return Room
        .databaseBuilder<SignalBriefDatabase>(
            context = applicationContext,
            name = applicationContext.getDatabasePath(DATABASE_FILE_NAME).absolutePath,
            factory = { SignalBriefDatabaseConstructor.initialize() },
        ).setDriver(BundledSQLiteDriver())
        .build()
}
