package com.recipesforsoftware.mvvm.di

import com.recipesforsoftware.mvvm.BuildConfig
import com.recipesforsoftware.mvvm.data.remote.NewsApiConfig
import com.recipesforsoftware.mvvm.data.remote.createHttpClient
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import javax.inject.Singleton

private const val BASE_URL = "https://newsapi.org/v2/"

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Provides
    @Singleton
    fun provideNewsApiConfig(): NewsApiConfig =
        NewsApiConfig(
            apiKey = BuildConfig.NEWS_API_KEY,
            baseUrl = BASE_URL,
        )

    @Provides
    @Singleton
    fun provideHttpClient(config: NewsApiConfig): HttpClient = createHttpClient(config)
}
