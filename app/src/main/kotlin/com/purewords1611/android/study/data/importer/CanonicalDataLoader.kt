package com.purewords1611.android.study.data.importer

import android.content.Context
import com.purewords1611.android.study.data.TestamentSection
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONArray

@Singleton
class CanonicalDataLoader @Inject constructor(
    @ApplicationContext private val context: Context
) {
    fun loadSources(): List<CanonicalSourceRecord> {
        val json = readAsset("study/sources_v1.json")
        val array = JSONArray(json)
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                add(
                    CanonicalSourceRecord(
                        sourceId = item.getString("source_id"),
                        label = item.getString("label")
                    )
                )
            }
        }
    }

    fun loadVerses(): List<CanonicalVerseRecord> {
        val files = context.assets.list("study") ?: emptyArray()
        android.util.Log.i("CanonicalDataLoader", "Found files in study asset folder: ${files.joinToString()}")
        val verseFiles = files.filter { it.startsWith("verses_") && it.endsWith(".json") }
        android.util.Log.i("CanonicalDataLoader", "Filtered verse files: ${verseFiles.joinToString()}")
        
        return buildList {
            verseFiles.forEach { fileName ->
                val json = readAsset("study/$fileName")
                val array = JSONArray(json)
                for (i in 0 until array.length()) {
                    val item = array.getJSONObject(i)
                    add(
                        CanonicalVerseRecord(
                            id = item.getLong("id"),
                            book = item.getString("book_display_name"),
                            bookOriginal = if (item.has("book_original_1611")) item.getString("book_original_1611") else null,
                            chapter = item.getInt("chapter"),
                            verse = item.getInt("verse"),
                            section = when(item.getString("testament_section")) {
                                "OT" -> TestamentSection.OLD_TESTAMENT
                                "APOC" -> TestamentSection.APOCRYPHA
                                "NT" -> TestamentSection.NEW_TESTAMENT
                                else -> TestamentSection.valueOf(item.getString("testament_section"))
                            },
                            canonicalOrder = item.getInt("canonical_order"),
                            originalText = item.getString("text_original_1611"),
                            modernizedText = item.getString("text_modernized_spelling"),
                            standardText = if (item.has("text_standard_kjv")) item.getString("text_standard_kjv") else item.getString("text_modernized_spelling"),
                            comparativeText = if (item.has("text_comparative_esv")) item.getString("text_comparative_esv") else null,
                            hasItalicWords = item.optBoolean("has_italicized_words", false),
                            sourceId = item.optString("source_id", "structure-placeholder"),
                            sourceLocator = item.optString("source_locator", ""),
                            checksumSha256 = item.optString("checksum_sha256", "00000000")
                        )
                    )
                }
            }
        }.sortedBy { it.canonicalOrder }
    }

    fun loadMarginalNotes(): List<CanonicalMarginalNoteRecord> {
        val json = readAsset("study/marginal_notes_1611.json")
        val array = JSONArray(json)
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                add(
                    CanonicalMarginalNoteRecord(
                        id = item.getLong("id"),
                        verseId = item.getLong("verse_id"),
                        noteType = item.getString("note_type"),
                        note = item.getString("note_text"),
                        anchorToken = if (item.has("anchor_token") && !item.isNull("anchor_token")) {
                            item.getString("anchor_token")
                        } else {
                            null
                        },
                        sourceId = item.getString("source_id"),
                        sourceLocator = item.getString("source_locator"),
                        checksumSha256 = item.getString("checksum_sha256")
                    )
                )
            }
        }
    }

    fun loadGlossary(): List<CanonicalGlossaryRecord> {
        val json = readAsset("study/glossary_v1.json")
        val array = JSONArray(json)
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                add(
                    CanonicalGlossaryRecord(
                        id = item.getString("entry_id"),
                        headword = item.getString("headword"),
                        definitionShort = item.getString("definition_short"),
                        sourceId = item.getString("source_id"),
                        checksumSha256 = item.getString("checksum_sha256")
                    )
                )
            }
        }
    }

    fun loadExplanations(): List<CanonicalExplanationRecord> {
        val json = readAsset("study/explanations_v1.json")
        val array = JSONArray(json)
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                add(
                    CanonicalExplanationRecord(
                        id = item.getString("explanation_id"),
                        verseId = item.getLong("verse_id"),
                        level = item.getString("level"),
                        contentMarkdown = item.getString("content_markdown"),
                        sourceId = item.getString("source_id"),
                        checksumSha256 = item.getString("checksum_sha256")
                    )
                )
            }
        }
    }

    fun loadSeekerTracks(): List<CanonicalSeekerTrackRecord> {
        val json = readAsset("study/seeker_tracks_v1.json")
        val array = JSONArray(json)
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                add(
                    CanonicalSeekerTrackRecord(
                        trackId = item.getString("track_id"),
                        title = item.getString("title"),
                        description = item.getString("description")
                    )
                )
            }
        }
    }

    fun loadSeekerSteps(): List<CanonicalSeekerStepRecord> {
        val json = readAsset("study/seeker_steps_v1.json")
        val array = JSONArray(json)
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                add(
                    CanonicalSeekerStepRecord(
                        stepId = item.getString("step_id"),
                        trackId = item.getString("track_id"),
                        sequence = item.getInt("sequence"),
                        title = item.getString("title"),
                        bodyMarkdown = item.getString("body_markdown")
                    )
                )
            }
        }
    }

    fun loadFrontMatter(): List<CanonicalFrontMatterRecord> {
        val json = readAsset("study/front_matter_v1.json")
        val array = JSONArray(json)
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                add(
                    CanonicalFrontMatterRecord(
                        docId = item.getString("doc_id"),
                        title = item.getString("title"),
                        textOriginal = item.getString("text_original"),
                        textModernized = item.getString("text_modernized_spelling"),
                        sourceId = item.getString("source_id"),
                        checksumSha256 = item.getString("checksum_sha256")
                    )
                )
            }
        }
    }

    private fun readAsset(path: String): String {
        return context.assets.open(path).bufferedReader().use { it.readText() }
    }
}
