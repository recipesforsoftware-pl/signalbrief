package com.recipesforsoftware.mvvm.di

import com.recipesforsoftware.mvvm.data.local.NewsLocalDataSource
import com.recipesforsoftware.mvvm.data.remote.KtorNewsRemoteDataSource
import com.recipesforsoftware.mvvm.data.remote.NewsRemoteDataSource
import com.recipesforsoftware.mvvm.data.repository.OfflineFirstNewsRepository
import com.recipesforsoftware.mvvm.domain.repository.NewsRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
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
