package com.recipesforsoftware.mvvm.data.local.db

/**
 * Creates a fresh, isolated database instance for tests.
 *
 * The actual implementation chooses an in-memory or temporary-file store
 * depending on the platform.
 */
expect fun createTestDatabase(): SignalBriefDatabase
