package com.recipesforsoftware.mvvm.di

import com.recipesforsoftware.mvvm.data.remote.KtorNewsRepository
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
    fun provideNewsRepository(client: HttpClient): NewsRepository = KtorNewsRepository(client)
}
