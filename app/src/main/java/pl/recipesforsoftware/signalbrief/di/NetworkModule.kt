package pl.recipesforsoftware.signalbrief.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import io.ktor.client.HttpClient
import pl.recipesforsoftware.signalbrief.BuildConfig
import pl.recipesforsoftware.signalbrief.data.remote.NewsApiConfig
import pl.recipesforsoftware.signalbrief.data.remote.createHttpClient
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
