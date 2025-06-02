package com.metromultindo.pdam_app_v2.di

import android.content.Context
import com.metromultindo.pdam_app_v2.data.api.ApiService
import com.metromultindo.pdam_app_v2.data.api.RetrofitInstance
import com.metromultindo.pdam_app_v2.data.datastore.UserPreferences
import com.metromultindo.pdam_app_v2.data.preferences.NewsPreferences
import com.metromultindo.pdam_app_v2.data.repository.BillRepository
import com.metromultindo.pdam_app_v2.data.repository.NewsRepository
import com.metromultindo.pdam_app_v2.services.LocationHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideApiService(): ApiService {
        return RetrofitInstance.api
    }

    @Provides
    @Singleton
    fun provideBillRepository(): BillRepository {
        return BillRepository()
    }

    @Provides
    @Singleton
    fun provideNewsRepository(apiService: ApiService): NewsRepository {
        return NewsRepository(apiService)
    }

    @Provides
    @Singleton
    fun provideUserPreferences(@ApplicationContext context: Context): UserPreferences {
        return UserPreferences(context)
    }

    @Provides
    @Singleton
    fun provideNewsPreferences(@ApplicationContext context: Context): NewsPreferences {
        return NewsPreferences(context)
    }

    @Provides
    @Singleton
    fun provideLocationHelper(
        @ApplicationContext context: Context
    ): LocationHelper {
        return LocationHelper(context)
    }

}