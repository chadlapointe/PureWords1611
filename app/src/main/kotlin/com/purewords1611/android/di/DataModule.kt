package com.purewords1611.android.di

import android.content.Context
import com.purewords1611.android.data.VerseRepository
import com.purewords1611.android.data.WordDictionary
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing data layer dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object DataModule {
    
    /**
     * Provides singleton instance of VerseRepository
     */
    @Provides
    @Singleton
    fun provideVerseRepository(
        @ApplicationContext context: Context
    ): VerseRepository {
        return VerseRepository(context)
    }
    
    /**
     * Provides singleton instance of WordDictionary
     */
    @Provides
    @Singleton
    fun provideWordDictionary(
        @ApplicationContext context: Context
    ): WordDictionary {
        return com.purewords1611.android.data.KjvWordDictionary(context)
    }
}
