package com.purewords1611.android.study.data.local

import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.io.IOException

@RunWith(AndroidJUnit4::class)
class Migration51To52Test {

    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        StudyDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    @Throws(IOException::class)
    fun testMigration51To52_empty_preservesData() {
        val db = helper.createDatabase(TEST_DB, 51)
        
        db.execSQL("INSERT INTO `highlights` (`id`, `verseId`, `colorName`, `createdAtEpochMillis`, `groupId`) VALUES (1, 100, 'yellow', 100000, 'group1')")
        db.close()

        val migratedDb = helper.runMigrationsAndValidate(TEST_DB, 52, true, MIGRATION_51_52)
        
        val cursor = migratedDb.query("SELECT * FROM `highlights` WHERE id = 1")
        assertEquals(true, cursor.moveToFirst())
        assertEquals(100, cursor.getInt(cursor.getColumnIndex("verseId")))
        assertEquals("yellow", cursor.getString(cursor.getColumnIndex("colorName")))
        assertEquals("group1", cursor.getString(cursor.getColumnIndex("groupId")))
        cursor.close()
    }
}
