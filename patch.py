import sys

file_path = "app/src/main/kotlin/com/purewords1611/android/study/data/local/ManualDatabaseInitializer.kt"

with open(file_path, "r", encoding="utf-8") as f:
    content = f.read()

# Add Companion Object
content = content.replace(
    'private val dbVersion = 52\n    private val identityHash = "b898f5b4176a2e64a70551e0de00e6eb"',
    'companion object {\n        const val EXPECTED_IDENTITY_HASH = "b898f5b4176a2e64a70551e0de00e6eb"\n        const val DB_VERSION = 52\n    }\n    private val dbVersion = DB_VERSION\n    private val identityHash = EXPECTED_IDENTITY_HASH'
)

# Patch ensureInitialized
old_ensure = """        if ((currentVersion < dbVersion) || (currentHash != identityHash) || (verseCount == 0)) {
            android.util.Log.i(tag, "Database needs re-initialization (version, hash mismatch, or empty verses table: $verseCount)")
            if (dbFile.exists()) {
                deleteDatabaseFiles(dbFile)
            }
            dbFile.parentFile?.mkdirs()
            copyAsset(dbFile)
            fixSchema(dbFile)"""

new_ensure = """        if ((currentVersion < dbVersion) || (currentHash != identityHash) || (verseCount == 0)) {
            android.util.Log.i(tag, "Database needs re-initialization (version, hash mismatch, or empty verses table: $verseCount)")
            val backupFile = File("${dbFile.absolutePath}.userbackup")
            if (dbFile.exists()) {
                SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
                    db.execSQL("PRAGMA wal_checkpoint(TRUNCATE)")
                }
                dbFile.copyTo(backupFile, overwrite = true)
                deleteDatabaseFiles(dbFile)
            }
            dbFile.parentFile?.mkdirs()
            copyAsset(dbFile)
            fixSchema(dbFile)
            if (backupFile.exists()) {
                restoreUserData(dbFile, backupFile)
            }"""

content = content.replace(old_ensure, new_ensure)

# Add restoreUserData helper
helper = """    private fun restoreUserData(newDbFile: File, backupFile: File) {
        val userTables = listOf("bookmarks", "highlights", "personal_notes", "chapter_completions", "reading_preferences", "study_stats", "marginalia")
        SQLiteDatabase.openDatabase(newDbFile.absolutePath, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
            db.execSQL("ATTACH DATABASE '${backupFile.absolutePath}' AS backup_db")
            db.beginTransaction()
            try {
                for (table in userTables) {
                    val cursor = db.rawQuery("SELECT name FROM backup_db.sqlite_master WHERE type='table' AND name='$table'", null)
                    val exists = cursor.moveToFirst()
                    cursor.close()
                    if (!exists) continue

                    val newCols = mutableListOf<String>()
                    db.rawQuery("PRAGMA table_info($table)", null).use { c ->
                        while (c.moveToNext()) { newCols.add(c.getString(1)) }
                    }
                    val oldCols = mutableListOf<String>()
                    db.rawQuery("PRAGMA backup_db.table_info($table)", null).use { c ->
                        while (c.moveToNext()) { oldCols.add(c.getString(1)) }
                    }
                    val commonCols = newCols.intersect(oldCols.toSet()).joinToString(",") { "`$it`" }
                    if (commonCols.isNotEmpty()) {
                        db.execSQL("INSERT OR REPLACE INTO `$table` ($commonCols) SELECT $commonCols FROM backup_db.`$table`")
                        val countCursor = db.rawQuery("SELECT changes()", null)
                        val count = if (countCursor.moveToFirst()) countCursor.getInt(0) else 0
                        countCursor.close()
                        android.util.Log.i(tag, "Restored $count rows into $table")
                    }
                }
                
                // Cleanup orphaned references across all user tables referencing verses
                db.execSQL("DELETE FROM bookmarks WHERE verseId NOT IN (SELECT id FROM verses)")
                db.execSQL("DELETE FROM highlights WHERE verseId NOT IN (SELECT id FROM verses)")
                db.execSQL("DELETE FROM personal_notes WHERE verseId NOT IN (SELECT id FROM verses)")
                
                db.setTransactionSuccessful()
            } catch (e: Exception) {
                android.util.Log.e(tag, "Error restoring user data", e)
                return // Exit before deleting the backup if restore fails
            } finally {
                db.endTransaction()
                db.execSQL("DETACH DATABASE backup_db")
            }
        }
        // ONLY delete the backup file if the entire transaction succeeded
        backupFile.delete()
    }

    private fun getIdentityHash"""

content = content.replace("    private fun getIdentityHash", helper)

with open(file_path, 'w', encoding='utf-8', newline='\n') as f:
    f.write(content)

print("Patch applied")
