package com.purewords1611.android.study.data.local

import org.junit.Assert.assertEquals
import org.junit.Test

class DatabaseMigrationTest {

    @Test
    fun testMigration50To51SQL() {
        assertEquals(50, MIGRATION_50_51.startVersion)
        assertEquals(51, MIGRATION_50_51.endVersion)
    }
}
