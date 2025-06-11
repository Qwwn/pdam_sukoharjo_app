package com.metromultindo.tirtamakmur.di

import android.content.Context
import com.metromultindo.tirtamakmur.data.api.ApiService
import com.metromultindo.tirtamakmur.data.api.RetrofitInstance
import com.metromultindo.tirtamakmur.data.datastore.UserPreferences
import com.metromultindo.tirtamakmur.data.preferences.NewsPreferences
import com.metromultindo.tirtamakmur.data.repository.BillRepository
import com.metromultindo.tirtamakmur.data.repository.NewsRepository
import com.metromultindo.tirtamakmur.services.LocationHelper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import com.metromultindo.tirtamakmur.data.repository.SelfMeterRepository

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

    @Provides
    @Singleton
    fun provideSelfMeterRepository(
        apiService: ApiService,
        @ApplicationContext context: Context
    ): SelfMeterRepository {
        return SelfMeterRepository(apiService, context)
    }

}