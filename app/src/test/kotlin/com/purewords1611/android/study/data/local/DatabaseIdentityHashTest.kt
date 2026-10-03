package com.purewords1611.android.study.data.local

import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import org.junit.Assert.assertTrue

class DatabaseIdentityHashTest {

    @Test
    fun testExpectedIdentityHashMatchesRoomGeneratedHash() {
        val expectedHash = ManualDatabaseInitializer.EXPECTED_IDENTITY_HASH
        
        val projectDir = System.getProperty("user.dir")
        val generatedDir = File(projectDir, "build/generated/ksp/debug/kotlin/com/purewords1611/android/study/data/local")
        
        val implFile = File(generatedDir, "StudyDatabase_Impl.kt")
        assertTrue("StudyDatabase_Impl.kt not found at ${implFile.absolutePath}. Ensure project is built.", implFile.exists())
        
        val content = implFile.readText()
        val regex = Regex("RoomOpenDelegate\\(\\d+,\\s*\"([a-f0-9]{32})\"")
        val match = regex.find(content)
        
        assertTrue("Could not parse Room identity hash from generated source", match != null)
        val generatedHash = match?.groupValues?.get(1) ?: ""
        
        assertEquals("ManualDatabaseInitializer hash does not match Room generated hash!", generatedHash, expectedHash)
    }
}
