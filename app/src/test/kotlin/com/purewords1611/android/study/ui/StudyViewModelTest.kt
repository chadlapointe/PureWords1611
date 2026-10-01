package com.purewords1611.android.study.ui

import com.purewords1611.android.study.data.StudyRepository
import com.purewords1611.android.study.service.PdfExportManager
import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class StudyViewModelTest {

    private lateinit viewModel: StudyViewModel
    private lateinit mockRepository: StudyRepository
    private lateinit mockPdfManager: PdfExportManager
    private lateinit mockContext: Context
    private lateinit mockAmbientPlayer: AmbientSoundPlayer

    @Before
    fun setup() {
        mockRepository = mock(StudyRepository::class.java)
        mockPdfManager = mock(PdfExportManager::class.java)
        mockContext = mock(Context::class.java)
        mockAmbientPlayer = mock(AmbientSoundPlayer::class.java)
        viewModel = StudyViewModel(mockRepository, mockAmbientPlayer, mockPdfManager, mockContext)
    }

    @Test
    fun resolveActionVerseIds_multiSelectBeatsActiveRange() {
        viewModel.toggleVerseSelection(42L)
        viewModel.toggleVerseSelection(43L)
        
        viewModel.selectVerseRange(1L, 10L, listOf(1L, 2L, 3L))
        
        val resolved = viewModel.resolveActionVerseIds()
        assertEquals(2, resolved.size)
        assertTrue(resolved.contains(42L))
        assertTrue(resolved.contains(43L))
    }

    @Test
    fun resolveActionVerseIds_fallbackToActiveRangeIfNoMultiSelect() {
        viewModel.selectVerseRange(1L, 10L, listOf(1L, 2L, 3L))
        
        val resolved = viewModel.resolveActionVerseIds()
        assertEquals(3, resolved.size)
        assertEquals(listOf(1L, 2L, 3L), resolved)
    }

    @Test
    fun resolveActionVerseIds_emptyIfNone() {
        val resolved = viewModel.resolveActionVerseIds()
        assertTrue(resolved.isEmpty())
    }

    @Test
    fun toggleVerseSelection_togglesProperly() {
        viewModel.toggleVerseSelection(42L)
        assertEquals(listOf(42L), viewModel.resolveActionVerseIds())
        
        viewModel.toggleVerseSelection(43L)
        assertEquals(2, viewModel.resolveActionVerseIds().size)
        
        viewModel.toggleVerseSelection(42L)
        assertEquals(listOf(43L), viewModel.resolveActionVerseIds())
    }
}
