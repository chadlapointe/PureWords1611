package com.purewords1611.android.study.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.purewords1611.android.study.data.TestamentSection

val MIGRATION_50_51 = object : Migration(50, 51) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE `highlights` ADD COLUMN `groupId` TEXT DEFAULT NULL")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_verses_lookup` ON `verses` (`section`, `book`, `chapter`, `id`)")
    }
}

@Database(
    entities = [
        VerseEntity::class,
        MarginalNoteEntity::class,
        BookmarkEntity::class,
        HighlightEntity::class,
        PersonalNoteEntity::class,
        ExplanationEntity::class,
        ReadingPreferenceEntity::class,
        VerseFtsEntity::class,
        FrontMatterEntity::class,
        ChapterCompletionEntity::class,
        ChapterSummaryEntity::class,
        VerseTitleEntity::class,
        LexiconEntity::class,
        StudyStatsEntity::class,
        MarginaliaEntity::class,
    ],
    version = 51,
    exportSchema = false,
)
@TypeConverters(StudyTypeConverters::class)
abstract class StudyDatabase : RoomDatabase() {
    abstract fun verseDao(): VerseDao
    abstract fun marginalNoteDao(): MarginalNoteDao
    abstract fun bookmarkDao(): BookmarkDao
    abstract fun highlightDao(): HighlightDao
    abstract fun personalNoteDao(): PersonalNoteDao
    abstract fun explanationDao(): ExplanationDao
    abstract fun readingPreferenceDao(): ReadingPreferenceDao
    abstract fun frontMatterDao(): FrontMatterDao
    abstract fun chapterCompletionDao(): ChapterCompletionDao
    abstract fun chapterSummaryDao(): ChapterSummaryDao
    abstract fun verseTitleDao(): VerseTitleDao
    abstract fun lexiconDao(): LexiconDao
    abstract fun studyStatsDao(): StudyStatsDao
    abstract fun marginaliaDao(): MarginaliaDao
}

object StudyTypeConverters {
    @TypeConverter
    fun fromSection(value: TestamentSection): String = value.name

    @TypeConverter
    fun toSection(value: String): TestamentSection = TestamentSection.valueOf(value)
}
