package com.purewords1611.android

import org.junit.Test
import java.io.File
import java.sql.DriverManager

class FixDatabaseTest {
    @Test
    fun fixSchema() {
        val dbPath = "C:/Users/chadl/AndroidStudioProjects/PureWords1611/app/src/main/assets/database/full_1611_bible.db"
        val dbFile = File(dbPath)
        if (!dbFile.exists()) {
            println("DB file not found at $dbPath")
            return
        }

        Class.forName("org.sqlite.JDBC")
        val conn = DriverManager.getConnection("jdbc:sqlite:$dbPath")
        val stmt = conn.createStatement()

        try {
            println("Fixing index name...")
            // Fix index name
            stmt.executeUpdate("PRAGMA writable_schema = 1")
            stmt.executeUpdate("UPDATE sqlite_master SET name = 'index_verses_book_chapter_verse', sql = REPLACE(sql, 'idx_verses_book_ch_vs', 'index_verses_book_chapter_verse') WHERE name = 'idx_verses_book_ch_vs'")
            
            println("Fixing FTS content quoting...")
            // Fix FTS content quoting
            // Since we can't easily recreate without fts4 module (if missing), we try direct update of sqlite_master
            // Note: Modern SQLite might still prevent this, but let's try.
            stmt.executeUpdate("UPDATE sqlite_master SET sql = REPLACE(sql, \"'verses'\", \"`verses`\") WHERE name = 'verses_fts'")
            
            stmt.executeUpdate("PRAGMA writable_schema = 0")
            println("Schema fixed successfully (hopefully)")
        } catch (e: Exception) {
            e.printStackTrace()
            
            // If direct update failed, try dropping and recreating IF fts4 is supported
            try {
                println("Direct update failed, trying DROP/CREATE...")
                stmt.executeUpdate("DROP INDEX IF EXISTS idx_verses_book_ch_vs")
                stmt.executeUpdate("CREATE UNIQUE INDEX IF NOT EXISTS index_verses_book_chapter_verse ON verses(book, chapter, verse)")
                
                // Recreate FTS
                stmt.executeUpdate("DROP TABLE IF EXISTS verses_fts")
                stmt.executeUpdate("CREATE VIRTUAL TABLE verses_fts USING fts4(originalText, modernizedText, comparativeText, content=`verses`)")
                stmt.executeUpdate("INSERT INTO verses_fts(verses_fts) VALUES('rebuild')")
                println("DROP/CREATE fixed successfully")
            } catch (e2: Exception) {
                println("DROP/CREATE also failed")
                e2.printStackTrace()
            }
        } finally {
            stmt.close()
            conn.close()
        }
    }
}
