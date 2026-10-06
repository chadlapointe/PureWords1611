package com.purewords1611.android.study.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DatabaseIdentityHashTest {

    @Test
    fun testExpectedIdentityHashMatchesRoomGeneratedHash() {
        val expectedHash = ManualDatabaseInitializer.EXPECTED_IDENTITY_HASH
        
        // user.dir in gradle unit tests is the module directory (app)
        val moduleDir = System.getProperty("user.dir")
        val schemaFile = File(moduleDir, "schemas/com.purewords1611.android.study.data.local.StudyDatabase/52.json")
        
        assertTrue("Schema 52.json not found at ${schemaFile.absolutePath}. Ensure room.schemaLocation is configured.", schemaFile.exists())
        
        val content = schemaFile.readText()
        val regex = Regex("\"identityHash\"\\s*:\\s*\"([a-f0-9]{32})\"")
        val match = regex.find(content)
        
        assertTrue("Could not parse Room identityHash from generated schema JSON", match != null)
        val generatedHash = match?.groupValues?.get(1) ?: ""
        
        assertEquals("ManualDatabaseInitializer hash does not match Room generated hash!", expectedHash, generatedHash)
    }
}
