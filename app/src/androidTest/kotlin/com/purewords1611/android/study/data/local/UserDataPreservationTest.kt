package com.purewords1611.android.study.data.local

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File
import android.database.sqlite.SQLiteDatabase

@RunWith(AndroidJUnit4::class)
class UserDataPreservationTest {

    private lateinit var context: Context
    private lateinit var dbFile: File
    private lateinit var stateHolder: DatabaseStateHolder
    private lateinit var initializer: ManualDatabaseInitializer

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        dbFile = context.getDatabasePath("pure_words_study.db")
        if (dbFile.exists()) dbFile.delete()

        stateHolder = DatabaseStateHolder()
        initializer = ManualDatabaseInitializer(context, stateHolder)

        dbFile.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(dbFile, null).use { db ->
            db.execSQL("CREATE TABLE room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
            db.execSQL("INSERT INTO room_master_table (id, identity_hash) VALUES (42, 'old_hash')")

            db.execSQL("CREATE TABLE verses (id INTEGER PRIMARY KEY, book TEXT NOT NULL, bookOriginal TEXT, chapter INTEGER NOT NULL, verse INTEGER NOT NULL, section TEXT NOT NULL, canonicalOrder INTEGER NOT NULL, originalText TEXT NOT NULL, modernizedText TEXT NOT NULL, hasItalicWords INTEGER NOT NULL, sourceId TEXT NOT NULL, sourceLocator TEXT NOT NULL, checksumSha256 TEXT NOT NULL, standardText TEXT, comparativeText TEXT, strongsText TEXT)")
            db.execSQL("INSERT INTO verses (id, book, chapter, verse, section, canonicalOrder, originalText, modernizedText, hasItalicWords, sourceId, sourceLocator, checksumSha256) VALUES (100, 'Genesis', 1, 1, 'OLD_TESTAMENT', 1, 'text', 'text', 0, 'src', 'loc', 'hash')")

            db.execSQL("CREATE TABLE bookmarks (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, verseId INTEGER NOT NULL, createdAtEpochMillis INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE highlights (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, verseId INTEGER NOT NULL, colorName TEXT NOT NULL, createdAtEpochMillis INTEGER NOT NULL, groupId TEXT)")
            db.execSQL("CREATE TABLE personal_notes (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, verseId INTEGER NOT NULL, note TEXT NOT NULL, updatedAtEpochMillis INTEGER NOT NULL, category TEXT)")
            db.execSQL("CREATE TABLE chapter_completions (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, book TEXT NOT NULL, chapter INTEGER NOT NULL, completedAtEpochMillis INTEGER NOT NULL, lastQuestionAskedAtEpochMillis INTEGER, masteryPoints INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE reading_preferences (id INTEGER NOT NULL, explanationLevel TEXT NOT NULL, contentVersion INTEGER NOT NULL, lastBook TEXT, lastChapter INTEGER, lastVerseId INTEGER, speechRate REAL NOT NULL DEFAULT 1.0, selectedVoice TEXT, PRIMARY KEY(id))")
            db.execSQL("CREATE TABLE study_stats (date TEXT NOT NULL, chaptersCompleted INTEGER NOT NULL, versesRead INTEGER NOT NULL, minutesSpent INTEGER NOT NULL, pointsEarned INTEGER NOT NULL, PRIMARY KEY(date))")
            db.execSQL("CREATE TABLE marginalia (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, book TEXT NOT NULL, chapter INTEGER NOT NULL, drawingJson TEXT NOT NULL, updatedAtEpochMillis INTEGER NOT NULL)")

            db.execSQL("INSERT INTO bookmarks (verseId, createdAtEpochMillis) VALUES (100, 12345)")
            db.execSQL("INSERT INTO highlights (verseId, colorName, createdAtEpochMillis, groupId) VALUES (100, 'yellow', 12345, null)")
            db.execSQL("INSERT INTO personal_notes (verseId, note, updatedAtEpochMillis, category) VALUES (100, 'My note', 12345, 'General')")
            db.execSQL("INSERT INTO chapter_completions (book, chapter, completedAtEpochMillis, masteryPoints) VALUES ('Genesis', 1, 12345, 10)")
            db.execSQL("INSERT INTO reading_preferences (id, explanationLevel, contentVersion, speechRate) VALUES (1, 'BEGINNER', 1, 1.0)")
            db.execSQL("INSERT INTO study_stats (date, chaptersCompleted, versesRead, minutesSpent, pointsEarned) VALUES ('2026-10-01', 1, 10, 5, 50)")
            db.execSQL("INSERT INTO marginalia (book, chapter, drawingJson, updatedAtEpochMillis) VALUES ('Genesis', 1, '{}', 12345)")

            db.version = 51
        }
    }

    @Test
    fun testEnsureInitializedPreservesUserDataAndUpdatesHash() = runBlocking {
        initializer.ensureInitialized()

        SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            db.rawQuery("SELECT identity_hash FROM room_master_table WHERE id = 42", null).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(ManualDatabaseInitializer.EXPECTED_IDENTITY_HASH, cursor.getString(0))
            }

            assertEquals(52, db.version)

            db.rawQuery("SELECT COUNT(*) FROM bookmarks", null).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(1, cursor.getInt(0))
            }
            db.rawQuery("SELECT COUNT(*) FROM highlights", null).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(1, cursor.getInt(0))
            }
            db.rawQuery("SELECT COUNT(*) FROM personal_notes", null).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(1, cursor.getInt(0))
            }
            db.rawQuery("SELECT COUNT(*) FROM chapter_completions", null).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(1, cursor.getInt(0))
            }
            db.rawQuery("SELECT COUNT(*) FROM reading_preferences", null).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(1, cursor.getInt(0))
            }
            db.rawQuery("SELECT COUNT(*) FROM study_stats", null).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(1, cursor.getInt(0))
            }
            db.rawQuery("SELECT COUNT(*) FROM marginalia", null).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(1, cursor.getInt(0))
            }
        }
    }

    @Test
    fun testEnsureInitializedCurrentVersionButMismatchHashPreservesData() = runBlocking {
        dbFile.delete()
        dbFile.parentFile?.mkdirs()
        SQLiteDatabase.openOrCreateDatabase(dbFile, null).use { db ->
            db.execSQL("CREATE TABLE room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
            db.execSQL("INSERT INTO room_master_table (id, identity_hash) VALUES (42, 'old_hash')")

            db.execSQL("CREATE TABLE verses (id INTEGER PRIMARY KEY, book TEXT NOT NULL, bookOriginal TEXT, chapter INTEGER NOT NULL, verse INTEGER NOT NULL, section TEXT NOT NULL, canonicalOrder INTEGER NOT NULL, originalText TEXT NOT NULL, modernizedText TEXT NOT NULL, hasItalicWords INTEGER NOT NULL, sourceId TEXT NOT NULL, sourceLocator TEXT NOT NULL, checksumSha256 TEXT NOT NULL, standardText TEXT, comparativeText TEXT, strongsText TEXT)")
            db.execSQL("INSERT INTO verses (id, book, chapter, verse, section, canonicalOrder, originalText, modernizedText, hasItalicWords, sourceId, sourceLocator, checksumSha256) VALUES (100, 'Genesis', 1, 1, 'OLD_TESTAMENT', 1, 'text', 'text', 0, 'src', 'loc', 'hash')")

            db.execSQL("CREATE TABLE bookmarks (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, verseId INTEGER NOT NULL, createdAtEpochMillis INTEGER NOT NULL)")
            db.execSQL("INSERT INTO bookmarks (verseId, createdAtEpochMillis) VALUES (100, 12345)")
            db.version = 52
        }

        initializer.ensureInitialized()

        SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            db.rawQuery("SELECT identity_hash FROM room_master_table WHERE id = 42", null).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(ManualDatabaseInitializer.EXPECTED_IDENTITY_HASH, cursor.getString(0))
            }

            db.rawQuery("SELECT COUNT(*) FROM bookmarks", null).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(1, cursor.getInt(0))
            }
        }
    }

    @Test
    fun testEnsureInitializedWithLeftoverBackupRestoresData() = runBlocking {
        val backupFile = File("${dbFile.absolutePath}.userbackup")
        if (backupFile.exists()) backupFile.delete()

        SQLiteDatabase.openOrCreateDatabase(backupFile, null).use { db ->
            db.execSQL("CREATE TABLE bookmarks (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, verseId INTEGER NOT NULL, createdAtEpochMillis INTEGER NOT NULL)")
            db.execSQL("INSERT INTO bookmarks (verseId, createdAtEpochMillis) VALUES (100, 12345)")
        }

        initializer.ensureInitialized()

        SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
            db.rawQuery("SELECT COUNT(*) FROM bookmarks", null).use { cursor ->
                assertTrue(cursor.moveToFirst())
                assertEquals(1, cursor.getInt(0))
            }
        }

        assertTrue("Backup file should be deleted on success", !backupFile.exists())
    }
}
