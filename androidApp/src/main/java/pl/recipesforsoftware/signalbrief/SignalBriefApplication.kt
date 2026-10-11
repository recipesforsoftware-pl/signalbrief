package pl.recipesforsoftware.signalbrief

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import pl.recipesforsoftware.signalbrief.data.remote.NewsApiConfig
import pl.recipesforsoftware.signalbrief.di.androidDataModule
import pl.recipesforsoftware.signalbrief.di.androidViewModelModule
import pl.recipesforsoftware.signalbrief.ui.di.signalBriefViewModelModule

class SignalBriefApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@SignalBriefApplication)
            modules(
                androidDataModule(
                    NewsApiConfig(apiKey = BuildConfig.NEWS_API_KEY, baseUrl = "https://newsapi.org/v2/"),
                ),
                signalBriefViewModelModule,
                androidViewModelModule,
            )
        }
    }
}
