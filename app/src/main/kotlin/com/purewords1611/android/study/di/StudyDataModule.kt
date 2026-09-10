package com.purewords1611.android.study.di

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import com.purewords1611.android.study.data.OfflineStudyRepository
import com.purewords1611.android.study.data.StudyRepository
import com.purewords1611.android.study.data.local.*
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object StudyDatabaseModule {

    @Provides
    @Singleton
    fun provideStudyDatabase(
        @ApplicationContext context: Context,
    ): StudyDatabase {
        val dbName = "pure_words_study.db"
        
        return Room.databaseBuilder(
            context,
            StudyDatabase::class.java,
            dbName,
        )
            .addMigrations(MIGRATION_50_51)
            .fallbackToDestructiveMigration()
            .setJournalMode(RoomDatabase.JournalMode.WRITE_AHEAD_LOGGING)
            .build()
    }

    @Provides
    fun provideVerseDao(database: StudyDatabase): VerseDao = database.verseDao()

    @Provides
    fun provideMarginalNoteDao(database: StudyDatabase): MarginalNoteDao = database.marginalNoteDao()

    @Provides
    fun provideExplanationDao(database: StudyDatabase): ExplanationDao = database.explanationDao()

    @Provides
    fun provideReadingPreferenceDao(database: StudyDatabase): ReadingPreferenceDao =
        database.readingPreferenceDao()

    @Provides
    fun provideBookmarkDao(database: StudyDatabase): BookmarkDao = database.bookmarkDao()

    @Provides
    fun provideHighlightDao(database: StudyDatabase): HighlightDao = database.highlightDao()

    @Provides
    fun providePersonalNoteDao(database: StudyDatabase): PersonalNoteDao = database.personalNoteDao()

    @Provides
    fun provideFrontMatterDao(database: StudyDatabase): FrontMatterDao = database.frontMatterDao()

    @Provides
    fun provideChapterCompletionDao(database: StudyDatabase): ChapterCompletionDao = database.chapterCompletionDao()

    @Provides
    fun provideChapterSummaryDao(database: StudyDatabase): ChapterSummaryDao = database.chapterSummaryDao()

    @Provides
    fun provideVerseTitleDao(database: StudyDatabase): VerseTitleDao = database.verseTitleDao()

    @Provides
    fun provideLexiconDao(database: StudyDatabase): LexiconDao = database.lexiconDao()

    @Provides
    fun provideStudyStatsDao(database: StudyDatabase): StudyStatsDao = database.studyStatsDao()

    @Provides
    fun provideMarginaliaDao(database: StudyDatabase): MarginaliaDao = database.marginaliaDao()
}

@Module
@InstallIn(SingletonComponent::class)
abstract class StudyRepositoryModule {
    @Binds
    @Singleton
    abstract fun bindStudyRepository(
        implementation: OfflineStudyRepository,
    ): StudyRepository
}
