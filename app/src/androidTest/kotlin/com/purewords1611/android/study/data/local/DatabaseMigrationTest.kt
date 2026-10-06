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
class DatabaseMigrationTest {

    private val TEST_DB = "migration-test"

    @get:Rule
    val helper: MigrationTestHelper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        StudyDatabase::class.java,
        emptyList(),
        FrameworkSQLiteOpenHelperFactory()
    )

    @Test
    fun testMigration50To51SQL() {
        assertEquals(50, MIGRATION_50_51.startVersion)
        assertEquals(51, MIGRATION_50_51.endVersion)
    }

    @Test
    @Throws(IOException::class)
    fun testMigration51To52_empty_preservesData() {
        // Create the database at version 51 using the schema exported at 52 (which is structurally identical)
        // Since we don't have 51.json, we use 52.json for v51 and ensure it migrates without dropping tables.
        val db = helper.createDatabase(TEST_DB, 51)
        
        // Insert a dummy highlight to verify preservation
        db.execSQL("INSERT INTO `highlights` (`id`, `verseId`, `colorName`, `createdAtEpochMillis`, `groupId`) VALUES (1, 100, 'yellow', 100000, 'group1')")
        db.close()

        // Run the migration to 52
        val migratedDb = helper.runMigrationsAndValidate(TEST_DB, 52, true, MIGRATION_51_52)
        
        // Verify the highlight is still there
        val cursor = migratedDb.query("SELECT * FROM `highlights` WHERE id = 1")
        assertEquals(true, cursor.moveToFirst())
        assertEquals(100, cursor.getInt(cursor.getColumnIndex("verseId")))
        assertEquals("yellow", cursor.getString(cursor.getColumnIndex("colorName")))
        assertEquals("group1", cursor.getString(cursor.getColumnIndex("groupId")))
        cursor.close()
    }
}
