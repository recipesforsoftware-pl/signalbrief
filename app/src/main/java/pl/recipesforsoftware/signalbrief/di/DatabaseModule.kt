package pl.recipesforsoftware.signalbrief.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import pl.recipesforsoftware.signalbrief.data.local.NewsLocalDataSource
import pl.recipesforsoftware.signalbrief.data.local.RoomNewsLocalDataSource
import pl.recipesforsoftware.signalbrief.data.local.db.SignalBriefDatabase
import pl.recipesforsoftware.signalbrief.data.local.db.createSignalBriefDatabase
import javax.inject.Singleton

/**
 * Hilt module for the shared Room database and its local data source.
 *
 * The database is a singleton: every request goes through the same
 * `SignalBriefDatabase` instance backed by the app's on-disk store. Room types
 * are used only here, at the Android composition root; they never leak into the
 * domain contract.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    @Provides
    @Singleton
    fun provideSignalBriefDatabase(
        @ApplicationContext context: Context,
    ): SignalBriefDatabase = createSignalBriefDatabase(context)

    @Provides
    @Singleton
    fun provideNewsLocalDataSource(database: SignalBriefDatabase): NewsLocalDataSource =
        RoomNewsLocalDataSource(
            database,
        )
}
