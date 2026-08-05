package pl.recipesforsoftware.signalbrief.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import pl.recipesforsoftware.signalbrief.data.local.NewsLocalDataSource
import pl.recipesforsoftware.signalbrief.data.remote.KtorNewsRemoteDataSource
import pl.recipesforsoftware.signalbrief.data.remote.NewsRemoteDataSource
import pl.recipesforsoftware.signalbrief.data.repository.OfflineFirstNewsRepository
import pl.recipesforsoftware.signalbrief.domain.repository.NewsRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {
    @Provides
    @Singleton
    fun provideNewsRemoteDataSource(client: HttpClient): NewsRemoteDataSource = KtorNewsRemoteDataSource(client)

    @Provides
    @Singleton
    fun provideNewsRepository(
        remoteDataSource: NewsRemoteDataSource,
        localDataSource: NewsLocalDataSource,
    ): NewsRepository = OfflineFirstNewsRepository(remoteDataSource, localDataSource)
}
