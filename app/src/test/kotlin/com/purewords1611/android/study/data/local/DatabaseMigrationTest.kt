package com.purewords1611.android.study.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

class DatabaseMigrationTest {

    @Test
    fun testMigration50To51SQL() {
        val sql = "ALTER TABLE `highlights` ADD COLUMN `groupId` TEXT DEFAULT NULL"
        assertEquals("ALTER TABLE `highlights` ADD COLUMN `groupId` TEXT DEFAULT NULL", sql)
        assertEquals(50, MIGRATION_50_51.startVersion)
        assertEquals(51, MIGRATION_50_51.endVersion)
    }
}
