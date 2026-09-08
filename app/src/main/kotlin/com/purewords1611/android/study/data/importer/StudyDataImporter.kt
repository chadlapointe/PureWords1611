package com.purewords1611.android.study.data.importer

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import javax.inject.Inject
import javax.inject.Singleton

import com.purewords1611.android.study.data.local.ExplanationDao
import com.purewords1611.android.study.data.local.ExplanationEntity
import com.purewords1611.android.study.data.local.MarginalNoteDao
import com.purewords1611.android.study.data.local.MarginalNoteEntity
import com.purewords1611.android.study.data.local.VerseDao
import com.purewords1611.android.study.data.local.FrontMatterDao
import com.purewords1611.android.study.data.local.FrontMatterEntity

@Singleton
class StudyDataImporter @Inject constructor(
    private val verseDao: VerseDao,
    private val marginalNoteDao: MarginalNoteDao,
    private val explanationDao: ExplanationDao,
    private val frontMatterDao: FrontMatterDao,
    private val canonicalDataLoader: CanonicalDataLoader,
) {
    private val _importProgress = MutableStateFlow(1.0f)
    val importProgress: StateFlow<Float> = _importProgress.asStateFlow()

    suspend fun importIfEmpty() = withContext(Dispatchers.IO) {
        val verseCount = verseDao.count()
        if (verseCount == 0) {
            android.util.Log.e("StudyDataImporter", "Verses table is empty! Stopping secondary import to avoid FK errors.")
            return@withContext
        }
        
        if (marginalNoteDao.count() == 0) {
            android.util.Log.i("StudyDataImporter", "Marginal notes empty, importing from JSON...")
            importMarginalNotes()
        }
        
        if (explanationDao.count() == 0) {
            android.util.Log.i("StudyDataImporter", "Explanations empty, importing from JSON...")
            importExplanations()
        }

        if (frontMatterDao.count() == 0) {
            android.util.Log.i("StudyDataImporter", "Front matter empty, importing from JSON...")
            importFrontMatter()
        }
    }

    private suspend fun importMarginalNotes() {
        _importProgress.value = 0.1f
        val notes = canonicalDataLoader.loadMarginalNotes().map {
            MarginalNoteEntity(
                id = it.id,
                verseId = it.verseId,
                noteType = it.noteType,
                note = it.note,
                anchorToken = it.anchorToken,
                sourceId = it.sourceId,
                sourceLocator = it.sourceLocator,
                checksumSha256 = it.checksumSha256,
            )
        }
        marginalNoteDao.upsertAll(notes)
        _importProgress.value = 0.5f
    }

    private suspend fun importExplanations() {
        val explanations = canonicalDataLoader.loadExplanations().map {
            ExplanationEntity(
                id = it.id,
                verseId = it.verseId,
                level = it.level,
                contentMarkdown = it.contentMarkdown,
                sourceId = it.sourceId,
                checksumSha256 = it.checksumSha256,
            )
        }
        explanationDao.upsertAll(explanations)
        _importProgress.value = 0.8f
    }

    private suspend fun importFrontMatter() {
        val items = canonicalDataLoader.loadFrontMatter().map {
            FrontMatterEntity(
                docId = it.docId,
                title = it.title,
                textOriginal = it.textOriginal,
                textModernizedSpelling = it.textModernized,
                sourceId = it.sourceId,
                checksumSha256 = it.checksumSha256,
            )
        }
        frontMatterDao.upsertAll(items)
        _importProgress.value = 1.0f
    }
}
