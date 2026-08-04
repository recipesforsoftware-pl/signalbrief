package com.recipesforsoftware.mvvm.data.local.db

import androidx.room.RoomDatabaseConstructor

/**
 * Expect constructor object required by Room for non-Android platforms. The
 * actual implementations instantiate the KSP-generated `_Impl` class.
 */
expect object SignalBriefDatabaseConstructor : RoomDatabaseConstructor<SignalBriefDatabase> {
    override fun initialize(): SignalBriefDatabase
}
