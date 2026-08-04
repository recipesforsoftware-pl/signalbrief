package com.recipesforsoftware.mvvm.data.local

/**
 * Stable filename for the on-device Room database. Kept in one place so Android
 * and iOS factories open the same store.
 */
internal const val DATABASE_FILE_NAME = "signalbrief.db"

/**
 * Feed identifier for the top-headlines cache. Country is stored separately, so
 * the same feed label can be cached for multiple countries.
 */
internal const val TOP_HEADLINES_FEED = "top-headlines"
