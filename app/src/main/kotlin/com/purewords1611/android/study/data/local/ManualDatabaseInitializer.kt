package com.purewords1611.android.study.data.local

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONObject
import org.json.JSONArray

@Singleton
class ManualDatabaseInitializer @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val databaseStateHolder: DatabaseStateHolder,
) {
    private val dbName = "pure_words_study.db"
    private val assetName = "database/full_1611_bible.db"
    private val dbVersion = 50
    private val identityHash = "7ea3d833e3a4a3f87a201b3f1149b959"
    private val mutex = Mutex()
    private val tag = "ManualDbInit_v50"

    suspend fun ensureInitialized(): Unit = mutex.withLock {
        val dbFile = context.getDatabasePath(dbName)
        val currentVersion = getVersion(dbFile)
        val currentHash = getIdentityHash(dbFile)

        android.util.Log.i(tag, "Starting database initialization for version $dbVersion (current: $currentVersion, currentHash: $currentHash, expectedHash: $identityHash)")

        if ((currentVersion < dbVersion) || (currentHash != identityHash)) {
            android.util.Log.i(tag, "Database needs re-initialization (version or hash mismatch)")
            if (dbFile.exists()) {
                deleteDatabaseFiles(dbFile)
            }
            dbFile.parentFile?.mkdirs()
            copyAsset(dbFile)
            fixSchema(dbFile)
            seedInitialSummaries(dbFile)
            seedInitialTitles(dbFile)
            populateAlternateTexts(dbFile)
            seedStrongsText(dbFile)
            seedLexicon(dbFile)
            rebuildFts(dbFile)
            android.util.Log.i(tag, "Database initialization complete")
        } else if (isDatabaseMissingTables(dbFile)) {
            android.util.Log.w(tag, "Database version matches but tables are missing. Fixing...")
            fixSchema(dbFile)
            seedInitialSummaries(dbFile)
            seedInitialTitles(dbFile)
            populateAlternateTexts(dbFile)
            seedStrongsText(dbFile)
            seedLexicon(dbFile)
            rebuildFts(dbFile)
        } else {
            android.util.Log.i(tag, "Database already initialized and healthy for version $dbVersion")
        }
        databaseStateHolder.setReady()
    }

    private fun getIdentityHash(dbFile: File): String? {
        if (!dbFile.exists()) return null
        return try {
            SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                val cursor = db.rawQuery("SELECT identity_hash FROM room_master_table WHERE id = 42", null)
                val hash = if (cursor.moveToFirst()) cursor.getString(0) else null
                cursor.close()
                hash
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun isDatabaseMissingTables(dbFile: File): Boolean {
        return try {
            SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { db ->
                val cursor = db.rawQuery("SELECT name FROM sqlite_master WHERE type='table' AND name='marginal_notes'", null)
                val missing = !cursor.moveToFirst()
                cursor.close()
                missing
            }
        } catch (e: Exception) {
            true
        }
    }

    private fun seedInitialSummaries(dbFile: File) {
        SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
            try {
                // Load Summaries
                val summariesAsset = try {
                    context.assets.open("study/chapter_summaries_v1.json")
                } catch (e: Exception) {
                    android.util.Log.e(tag, "Summaries asset not found", e)
                    null
                }
                
                if (summariesAsset != null) {
                    val summariesJson = summariesAsset.bufferedReader().use { it.readText() }
                    val summariesArray = JSONArray(summariesJson)
                    
                    // Load Section Headers (to join manually since they are small)
                    val sectionsJson = context.assets.open("study/section_headers_v1.json").bufferedReader().use { it.readText() }
                    val sectionsArray = JSONArray(sectionsJson)
                    val sectionsMap = mutableMapOf<String, String>()
                    for (j in 0 until sectionsArray.length()) {
                        val sObj = sectionsArray.getJSONObject(j)
                        sectionsMap[sObj.getString("book")] = sObj.getString("section_title")
                    }

                    db.beginTransaction()
                    try {
                        for (i in 0 until summariesArray.length()) {
                            val obj = summariesArray.getJSONObject(i)
                            val book = obj.getString("book")
                            val chapter = obj.getInt("chapter")
                            val s1611 = if (obj.isNull("summary1611")) null else obj.getString("summary1611")
                            val tEsv = if (obj.isNull("titleEsv")) null else obj.getString("titleEsv")
                            val tStd = if (obj.isNull("titleStandard")) null else obj.getString("titleStandard")
                            val sTitle = sectionsMap[book]
                            
                            db.execSQL(
                                "INSERT OR REPLACE INTO chapter_summaries (book, chapter, summary1611, titleEsv, titleStandard, sectionTitle) VALUES (?, ?, ?, ?, ?, ?)",
                                arrayOf<Any?>(book, chapter, s1611, tEsv, tStd, sTitle),
                            )
                        }
                        db.setTransactionSuccessful()
                    } finally {
                        db.endTransaction()
                    }
                    android.util.Log.i(tag, "Populated ${summariesArray.length()} chapter summaries with section titles")
                }
            } catch (e: Exception) {
                android.util.Log.e(tag, "Error seeding summaries", e)
            }
        }
    }

    private fun seedInitialTitles(dbFile: File) {
        SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
            try {
                val json = context.assets.open("study/verse_titles_v1.json").bufferedReader().use { it.readText() }
                val array = JSONArray(json)
                db.beginTransaction()
                try {
                    for (i in 0 until array.length()) {
                        val obj = array.getJSONObject(i)
                        db.execSQL(
                            "INSERT OR REPLACE INTO verse_titles (book, chapter, verse, title, translation) VALUES (?, ?, ?, ?, ?)",
                            arrayOf(
                                obj.getString("book"),
                                obj.getInt("chapter"),
                                obj.getInt("verse"),
                                obj.getString("title"),
                                obj.getString("translation")
                            )
                        )
                    }
                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
                android.util.Log.i(tag, "Populated ${array.length()} verse titles")
            } catch (e: Exception) {
                android.util.Log.e(tag, "Error seeding verse titles", e)
            }
        }
    }

    private fun populateAlternateTexts(dbFile: File) {
        SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
            try {
                val bibleBooks = mutableSetOf<String>()
                db.rawQuery("SELECT DISTINCT book FROM verses", null).use { cursor ->
                    while (cursor.moveToNext()) bibleBooks.add(cursor.getString(0))
                }

                fun normalize(name: String): String {
                    val n = name.replace("I ", "1 ")
                        .replace("II ", "2 ")
                        .replace("III ", "3 ")
                        .replace("Song of Songs", "Song of Solomon")
                        .replace("Psalm", "Psalms")
                    return if (n == "Psalms" && !bibleBooks.contains("Psalms") && bibleBooks.contains("Psalm")) "Psalm"
                    else if (n == "Psalms" && bibleBooks.contains("Psalms")) "Psalms"
                    else n
                }

                // Populate Standard KJV from kjv.json
                val kjvJson = context.assets.open("study/kjv.json").bufferedReader().use { it.readText() }
                val kjvArray = JSONObject(kjvJson).getJSONArray("verses")
                db.beginTransaction()
                try {
                    var count = 0
                    val stmt = db.compileStatement("UPDATE verses SET standardText = ? WHERE book = ? AND chapter = ? AND verse = ?")
                    for (i in 0 until kjvArray.length()) {
                        val v = kjvArray.getJSONObject(i)
                        val bookName = normalize(v.getString("book_name"))
                        val ch = v.getInt("chapter")
                        val vs = v.getInt("verse")
                        val text = v.getString("text")
                        
                        stmt.clearBindings()
                        stmt.bindString(1, text)
                        stmt.bindString(2, bookName)
                        stmt.bindLong(3, ch.toLong())
                        stmt.bindLong(4, vs.toLong())
                        val affected = stmt.executeUpdateDelete()
                        if (affected > 0) count++
                    }
                    db.setTransactionSuccessful()
                    android.util.Log.i(tag, "Populated Standard KJV text for $count verses")
                } finally {
                    db.endTransaction()
                }

                // Populate ESV from ESV.json
                val esvJson = context.assets.open("study/ESV.json").bufferedReader().use { it.readText() }
                val esvObj = JSONObject(esvJson).getJSONObject("books")
                db.beginTransaction()
                try {
                    var count = 0
                    val stmt = db.compileStatement("UPDATE verses SET comparativeText = ? WHERE book = ? AND chapter = ? AND verse = ?")
                    esvObj.keys().forEach { rawBookName ->
                        val bookName = normalize(rawBookName)
                        val bookArr = esvObj.getJSONArray(rawBookName)
                        for (cIdx in 0 until bookArr.length()) {
                            val chapterArr = bookArr.getJSONArray(cIdx)
                            for (vIdx in 0 until chapterArr.length()) {
                                val verseWords = chapterArr.getJSONArray(vIdx)
                                val verseText = StringBuilder()
                                for (wIdx in 0 until verseWords.length()) {
                                    val wordPair = verseWords.getJSONArray(wIdx)
                                    val token = wordPair.getString(0)
                                    if (verseText.isNotEmpty() && token.isNotEmpty()) {
                                        val firstChar = token[0]
                                        if (firstChar.isLetterOrDigit() || firstChar == '(' || firstChar == '[' || firstChar == '{' || firstChar == '"' || firstChar == '\'') {
                                            verseText.append(" ")
                                        }
                                    }
                                    verseText.append(token)
                                }
                                
                                stmt.clearBindings()
                                stmt.bindString(1, verseText.toString().trim())
                                stmt.bindString(2, bookName)
                                stmt.bindLong(3, (cIdx + 1).toLong())
                                stmt.bindLong(4, (vIdx + 1).toLong())
                                val affected = stmt.executeUpdateDelete()
                                if (affected > 0) count++
                            }
                        }
                    }
                    db.setTransactionSuccessful()
                    android.util.Log.i(tag, "Populated ESV text for $count verses")
                } finally {
                    db.endTransaction()
                }
            } catch (e: Exception) {
                android.util.Log.e(tag, "Error populating alternate texts", e)
            }
        }
    }

    private fun getVersion(dbFile: File): Int {
        if (!dbFile.exists()) return -1
        return try {
            SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READONLY).use { it.version }
        } catch (e: Exception) {
            android.util.Log.e(tag, "Error reading database version", e)
            -1
        }
    }

    private fun deleteDatabaseFiles(dbFile: File) {
        dbFile.delete()
        File("${dbFile.absolutePath}-shm").delete()
        File("${dbFile.absolutePath}-wal").delete()
    }

    private fun copyAsset(dbFile: File) {
        context.assets.open(assetName).use { input ->
            FileOutputStream(dbFile).use { output ->
                input.copyTo(output)
            }
        }
        android.util.Log.i(tag, "Asset copied. Size: ${dbFile.length()} bytes")
    }

    private fun fixSchema(dbFile: File) {
        val b = "`"
        SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
            db.execSQL("PRAGMA foreign_keys=OFF")
            
            // 1. Create missing tables with exact Room order and constraints
            db.execSQL("CREATE TABLE IF NOT EXISTS ${b}marginal_notes$b (${b}id$b INTEGER NOT NULL, ${b}verseId$b INTEGER NOT NULL, ${b}noteType$b TEXT NOT NULL, ${b}note$b TEXT NOT NULL, ${b}anchorToken$b TEXT, ${b}sourceId$b TEXT NOT NULL, ${b}sourceLocator$b TEXT NOT NULL, ${b}checksumSha256$b TEXT NOT NULL, PRIMARY KEY(${b}id$b), FOREIGN KEY(${b}verseId$b) REFERENCES ${b}verses$b(${b}id$b) ON UPDATE NO ACTION ON DELETE CASCADE )")
            db.execSQL("CREATE INDEX IF NOT EXISTS ${b}index_marginal_notes_verseId$b ON ${b}marginal_notes$b (${b}verseId$b)")
            db.execSQL("CREATE TABLE IF NOT EXISTS ${b}bookmarks$b (${b}id$b INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, ${b}verseId$b INTEGER NOT NULL, ${b}createdAtEpochMillis$b INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE IF NOT EXISTS ${b}highlights$b (${b}id$b INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, ${b}verseId$b INTEGER NOT NULL, ${b}colorName$b TEXT NOT NULL, ${b}createdAtEpochMillis$b INTEGER NOT NULL)")
            db.execSQL("CREATE TABLE IF NOT EXISTS ${b}personal_notes$b (${b}id$b INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, ${b}verseId$b INTEGER NOT NULL, ${b}note$b TEXT NOT NULL, ${b}updatedAtEpochMillis$b INTEGER NOT NULL, ${b}category$b TEXT)")
            db.execSQL("CREATE TABLE IF NOT EXISTS ${b}explanations$b (${b}id$b TEXT NOT NULL, ${b}verseId$b INTEGER NOT NULL, ${b}level$b TEXT NOT NULL, ${b}contentMarkdown$b TEXT NOT NULL, ${b}sourceId$b TEXT NOT NULL, ${b}checksumSha256$b TEXT NOT NULL, PRIMARY KEY(${b}id$b), FOREIGN KEY(${b}verseId$b) REFERENCES ${b}verses$b(${b}id$b) ON UPDATE NO ACTION ON DELETE CASCADE )")
            db.execSQL("CREATE INDEX IF NOT EXISTS ${b}index_explanations_verseId$b ON ${b}explanations$b (${b}verseId$b)")
            db.execSQL("CREATE INDEX IF NOT EXISTS ${b}index_explanations_level$b ON ${b}explanations$b (${b}level$b)")
            db.execSQL("CREATE TABLE IF NOT EXISTS ${b}reading_preferences$b (${b}id$b INTEGER NOT NULL, ${b}explanationLevel$b TEXT NOT NULL, ${b}contentVersion$b INTEGER NOT NULL, ${b}lastBook$b TEXT, ${b}lastChapter$b INTEGER, ${b}lastVerseId$b INTEGER, ${b}speechRate$b REAL NOT NULL DEFAULT 1.0, ${b}selectedVoice$b TEXT, PRIMARY KEY(${b}id$b))")
            db.execSQL("CREATE TABLE IF NOT EXISTS ${b}front_matter$b (${b}docId$b TEXT NOT NULL, ${b}title$b TEXT NOT NULL, ${b}textOriginal$b TEXT NOT NULL, ${b}textModernizedSpelling$b TEXT NOT NULL, ${b}sourceId$b TEXT NOT NULL, ${b}checksumSha256$b TEXT NOT NULL, PRIMARY KEY(${b}docId$b))")
            db.execSQL("CREATE TABLE IF NOT EXISTS ${b}chapter_completions$b (${b}id$b INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, ${b}book$b TEXT NOT NULL, ${b}chapter$b INTEGER NOT NULL, ${b}completedAtEpochMillis$b INTEGER NOT NULL, ${b}lastQuestionAskedAtEpochMillis$b INTEGER, ${b}masteryPoints$b INTEGER NOT NULL)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS ${b}index_chapter_completions_book_chapter$b ON ${b}chapter_completions$b (${b}book$b, ${b}chapter$b)")
            db.execSQL("CREATE TABLE IF NOT EXISTS ${b}chapter_summaries$b (${b}id$b INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, ${b}book$b TEXT NOT NULL, ${b}chapter$b INTEGER NOT NULL, ${b}summary1611$b TEXT, ${b}titleEsv$b TEXT, ${b}titleStandard$b TEXT, ${b}sectionTitle$b TEXT)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS ${b}index_chapter_summaries_book_chapter$b ON ${b}chapter_summaries$b (${b}book$b, ${b}chapter$b)")
            db.execSQL("CREATE TABLE IF NOT EXISTS ${b}verse_titles$b (${b}id$b INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, ${b}book$b TEXT NOT NULL, ${b}chapter$b INTEGER NOT NULL, ${b}verse$b INTEGER NOT NULL, ${b}title$b TEXT NOT NULL, ${b}translation$b TEXT NOT NULL)")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS ${b}index_verse_titles_book_chapter_verse_translation$b ON ${b}verse_titles$b (${b}book$b, ${b}chapter$b, ${b}verse$b, ${b}translation$b)")
            db.execSQL("CREATE TABLE IF NOT EXISTS ${b}lexicon$b (${b}strongsId$b TEXT NOT NULL, ${b}word$b TEXT, ${b}transliteration$b TEXT, ${b}pronunciation$b TEXT, ${b}definition$b TEXT, ${b}info$b TEXT, PRIMARY KEY(${b}strongsId$b))")
            db.execSQL("CREATE TABLE IF NOT EXISTS ${b}study_stats$b (${b}date$b TEXT NOT NULL, ${b}chaptersCompleted$b INTEGER NOT NULL, ${b}versesRead$b INTEGER NOT NULL, ${b}minutesSpent$b INTEGER NOT NULL, ${b}pointsEarned$b INTEGER NOT NULL, PRIMARY KEY(${b}date$b))")
            db.execSQL("CREATE TABLE IF NOT EXISTS ${b}marginalia$b (${b}id$b INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, ${b}book$b TEXT NOT NULL, ${b}chapter$b INTEGER NOT NULL, ${b}drawingJson$b TEXT NOT NULL, ${b}updatedAtEpochMillis$b INTEGER NOT NULL)")

            // 2. Fix verses table
            try {
                if (!columnExists(db, "verses", "standardText")) {
                    db.execSQL("ALTER TABLE ${b}verses$b ADD COLUMN ${b}standardText$b TEXT")
                }
            } catch (e: Exception) {
                android.util.Log.w(tag, "Failed to add standardText column", e)
            }
            try {
                if (!columnExists(db, "verses", "comparativeText")) {
                    db.execSQL("ALTER TABLE ${b}verses$b ADD COLUMN ${b}comparativeText$b TEXT")
                }
            } catch (e: Exception) {
                android.util.Log.w(tag, "Failed to add comparativeText column", e)
            }
            try {
                if (!columnExists(db, "verses", "strongsText")) {
                    db.execSQL("ALTER TABLE ${b}verses$b ADD COLUMN ${b}strongsText$b TEXT")
                }
            } catch (e: Exception) {
                android.util.Log.w(tag, "Failed to add strongsText column", e)
            }
            db.execSQL("DROP INDEX IF EXISTS idx_verses_book_ch_vs")
            db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_verses_book_chapter_verse ON verses(book, chapter, verse)")
            db.execSQL("CREATE INDEX IF NOT EXISTS index_verses_lookup ON verses(section, book, chapter, id)")

            // 3. Fix reading_preferences
            try {
                if (!columnExists(db, "reading_preferences", "speechRate")) {
                    db.execSQL("ALTER TABLE ${b}reading_preferences$b ADD COLUMN ${b}speechRate$b REAL NOT NULL DEFAULT 1.0")
                }
            } catch (e: Exception) {
                android.util.Log.w(tag, "Failed to add speechRate column", e)
            }
            try {
                if (!columnExists(db, "reading_preferences", "selectedVoice")) {
                    db.execSQL("ALTER TABLE ${b}reading_preferences$b ADD COLUMN ${b}selectedVoice$b TEXT")
                }
            } catch (e: Exception) {
                android.util.Log.w(tag, "Failed to add selectedVoice column", e)
            }
            try {
                if (!columnExists(db, "personal_notes", "category")) {
                    db.execSQL("ALTER TABLE ${b}personal_notes$b ADD COLUMN ${b}category$b TEXT")
                }
            } catch (e: Exception) {
                android.util.Log.w(tag, "Failed to add category column to personal_notes", e)
            }

            // 4. Fix FTS table
            android.util.Log.i(tag, "Fixing FTS table...")
            db.execSQL("DROP TABLE IF EXISTS ${b}verses_fts$b")
            db.execSQL("CREATE VIRTUAL TABLE ${b}verses_fts$b USING FTS4(${b}book$b TEXT NOT NULL, ${b}originalText$b TEXT NOT NULL, ${b}modernizedText$b TEXT NOT NULL, ${b}standardText$b TEXT, ${b}comparativeText$b TEXT, ${b}strongsText$b TEXT, content=${b}verses$b)")

            // 4. Identity Hash
            android.util.Log.i(tag, "Setting Identity Hash to $identityHash and version to $dbVersion")
            db.execSQL("CREATE TABLE IF NOT EXISTS room_master_table (id INTEGER PRIMARY KEY,identity_hash TEXT)")
            db.execSQL("DELETE FROM room_master_table")
            db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(42, '$identityHash')")
            // Also insert at id=1 just in case some Room versions expect it there
            db.execSQL("INSERT OR REPLACE INTO room_master_table (id,identity_hash) VALUES(1, '$identityHash')")
            db.version = dbVersion
            android.util.Log.i(tag, "Schema fix completed successfully")
        }
    }

    private fun seedStrongsText(dbFile: File) {
        SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
            try {
                val json = context.assets.open("study/kjv_strongs.json").bufferedReader().use { it.readText() }
                val array = JSONObject(json).getJSONArray("verses")
                db.beginTransaction()
                try {
                    var count = 0
                    val stmt = db.compileStatement("UPDATE verses SET strongsText = ? WHERE book = ? AND chapter = ? AND verse = ?")
                    for (i in 0 until array.length()) {
                        val v = array.getJSONObject(i)
                        val bookName = v.getString("book_name")
                        val ch = v.getInt("chapter")
                        val vs = v.getInt("verse")
                        val text = v.getString("text")
                        
                        stmt.clearBindings()
                        stmt.bindString(1, text)
                        stmt.bindString(2, bookName)
                        stmt.bindLong(3, ch.toLong())
                        stmt.bindLong(4, vs.toLong())
                        val affected = stmt.executeUpdateDelete()
                        if (affected > 0) count++
                    }
                    db.setTransactionSuccessful()
                    android.util.Log.i(tag, "Populated Strongs KJV text for $count verses")
                } finally {
                    db.endTransaction()
                }
            } catch (e: Exception) {
                android.util.Log.e(tag, "Error seeding strongs text", e)
            }
        }
    }

    private fun seedLexicon(dbFile: File) {
        SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
            try {
                db.beginTransaction()
                try {
                    // 1. Hebrew
                    val hebrewJson = context.assets.open("study/strongs_hebrew.json").bufferedReader().use { it.readText() }
                    val hebrewObj = JSONObject(hebrewJson)
                    hebrewObj.keys().forEach { id ->
                        try {
                            val entry = hebrewObj.getJSONObject(id)
                            val word = entry.optString("lemma")
                            val translit = entry.optString("xlit")
                            val pron = entry.optString("pron")
                            val sDef = entry.optString("strongs_def")
                            val kDef = entry.optString("kjv_def")
                            val fullDef = if (kDef.isNotEmpty()) "$sDef\n\nKJV: $kDef" else sDef
                            val info = entry.optString("derivation")
                            
                            db.execSQL(
                                "INSERT OR REPLACE INTO lexicon (strongsId, word, transliteration, pronunciation, definition, info) VALUES (?, ?, ?, ?, ?, ?)",
                                arrayOf(id, word, translit, pron, fullDef, info)
                            )
                        } catch (e: Exception) {
                            android.util.Log.w(tag, "Failed to parse Hebrew entry $id", e)
                        }
                    }

                    // 2. Greek
                    val greekJson = context.assets.open("study/strongs_greek.json").bufferedReader().use { it.readText() }
                    val greekObj = JSONObject(greekJson)
                    greekObj.keys().forEach { id ->
                        try {
                            val entry = greekObj.getJSONObject(id)
                            val word = entry.optString("lemma")
                            val translit = entry.optString("translit")
                            val pron = "" // Greek dataset uses translit for both usually
                            val sDef = entry.optString("strongs_def")
                            val kDef = entry.optString("kjv_def")
                            val fullDef = if (kDef.isNotEmpty()) "$sDef\n\nKJV: $kDef" else sDef
                            val info = entry.optString("derivation")
                            
                            db.execSQL(
                                "INSERT OR REPLACE INTO lexicon (strongsId, word, transliteration, pronunciation, definition, info) VALUES (?, ?, ?, ?, ?, ?)",
                                arrayOf(id, word, translit, pron, fullDef, info)
                            )
                        } catch (e: Exception) {
                            android.util.Log.w(tag, "Failed to parse Greek entry $id", e)
                        }
                    }

                    // 3. Morphology
                    val tvmJson = context.assets.open("study/tvm_definitions.json").bufferedReader().use { it.readText() }
                    val morphArray = JSONObject(tvmJson).getJSONArray("morphology")
                    for (i in 0 until morphArray.length()) {
                        try {
                            val entry = morphArray.getJSONObject(i)
                            val id = entry.getString("code")
                            val word = entry.getString("label")
                            val def = entry.getString("description")
                            
                            db.execSQL(
                                "INSERT OR REPLACE INTO lexicon (strongsId, word, transliteration, pronunciation, definition, info) VALUES (?, ?, ?, ?, ?, ?)",
                                arrayOf(id, word, "", "", def, "Morphology")
                            )
                        } catch (e: Exception) {
                            android.util.Log.w(tag, "Failed to parse Morphology entry at index $i", e)
                        }
                    }

                    db.setTransactionSuccessful()
                } finally {
                    db.endTransaction()
                }
                android.util.Log.i(tag, "Populated Lexicon table (Hebrew, Greek, and Morphology)")
            } catch (e: Exception) {
                android.util.Log.e(tag, "Error seeding lexicon", e)
            }
        }
    }

    private fun columnExists(db: SQLiteDatabase, tableName: String, columnName: String): Boolean {
        db.rawQuery("PRAGMA table_info($tableName)", null).use { cursor ->
            val nameIndex = cursor.getColumnIndex("name")
            while (cursor.moveToNext()) {
                if (cursor.getString(nameIndex) == columnName) return true
            }
        }
        return false
    }

    private fun rebuildFts(dbFile: File) {
        SQLiteDatabase.openDatabase(dbFile.absolutePath, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
            db.execSQL("INSERT INTO verses_fts(verses_fts) VALUES('rebuild')")
            android.util.Log.i(tag, "FTS table rebuilt and synchronized")
        }
    }
}
