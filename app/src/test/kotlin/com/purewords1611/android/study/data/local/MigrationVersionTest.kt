package com.purewords1611.android.study.data.local

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import org.json.JSONObject

class MigrationVersionTest {

    @Test
    fun testExpectedIdentityHashMatchesRoomGeneratedHash() {
        val expectedHash = ManualDatabaseInitializer.EXPECTED_IDENTITY_HASH
        
        var baseDir = System.getProperty("user.dir") ?: ""
        if (!baseDir.endsWith("app")) {
            baseDir = "$baseDir/app"
        }
        val schemaFile = File(baseDir, "schemas/com.purewords1611.android.study.data.local.StudyDatabase/52.json")
        val schemaFile51 = File(baseDir, "schemas/com.purewords1611.android.study.data.local.StudyDatabase/51.json")
        
        assertTrue("Schema 52.json not found", schemaFile.exists())
        assertTrue("Schema 51.json not found", schemaFile51.exists())
        
        val content = schemaFile.readText()
        val regex = Regex("\"identityHash\"\\s*:\\s*\"([a-f0-9]{32})\"")
        val match = regex.find(content)
        
        assertTrue("Could not parse Room identityHash from generated schema JSON", match != null)
        val generatedHash = match?.groupValues?.get(1) ?: ""
        
        assertEquals("ManualDatabaseInitializer hash does not match Room generated hash!", expectedHash, generatedHash)
        
        val content51 = schemaFile51.readText()
        val match51 = regex.find(content51)
        assertTrue(match51 != null)
        assertEquals(generatedHash, match51?.groupValues?.get(1) ?: "")
    }
}
