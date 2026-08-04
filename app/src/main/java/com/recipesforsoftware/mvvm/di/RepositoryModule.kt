package com.recipesforsoftware.mvvm.di

import com.recipesforsoftware.mvvm.data.repository.TopHeadlineRepository
import com.recipesforsoftware.mvvm.domain.repository.NewsRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    @Binds
    @Singleton
    abstract fun bindNewsRepository(repository: TopHeadlineRepository): NewsRepository
}
