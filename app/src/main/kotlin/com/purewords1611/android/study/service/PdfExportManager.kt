package com.purewords1611.android.study.service

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.os.Environment
import androidx.core.content.res.ResourcesCompat
import com.purewords1611.android.R
import com.purewords1611.android.study.data.DrawingPath
import com.purewords1611.android.study.data.VerseText
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
open class PdfExportManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
) {
    fun exportChapter(
        book: String,
        chapter: Int,
        verses: List<VerseText>,
        marginalia: List<DrawingPath>
    ): File? {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // A4 size
        val page = pdfDocument.startPage(pageInfo)
        val canvas: Canvas = page.canvas
        
        val paint = Paint()
        val blackletterTypeface = ResourcesCompat.getFont(context, R.font.kjva6aa)
        
        // Background - Parchment color
        canvas.drawColor(0xFFF4ECD8.toInt())
        
        // 1611 Border lines
        paint.color = Color.BLACK
        paint.strokeWidth = 1f
        paint.style = Paint.Style.STROKE
        canvas.drawRect(40f, 40f, 555f, 802f, paint)
        canvas.drawRect(45f, 45f, 550f, 797f, paint)
        
        // Title
        paint.style = Paint.Style.FILL
        paint.typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        paint.textSize = 24f
        canvas.drawText(book.uppercase(), 100f, 80f, paint)
        
        paint.typeface = Typeface.create(Typeface.SERIF, Typeface.ITALIC)
        paint.textSize = 18f
        canvas.drawText("Chapter $chapter", 100f, 110f, paint)
        
        // Verses
        paint.typeface = blackletterTypeface ?: Typeface.DEFAULT
        paint.textSize = 12f
        var y = 150f
        val margin = 80f
        val maxWidth = 435f
        
        verses.take(30).forEach { verse ->
            val text = "${verse.verse} ${verse.originalText}"
            val lines = breakText(text, maxWidth, paint)
            lines.forEach { line ->
                if (y > 750f) return@forEach // Basic overflow check
                canvas.drawText(line, margin, y, paint)
                y += 20f
            }
            y += 10f
        }

        // Draw Marginalia
        paint.style = Paint.Style.STROKE
        marginalia.forEach { path ->
            paint.color = path.color
            paint.strokeWidth = path.strokeWidth / 2f // Scale down for PDF
            val androidPath = android.graphics.Path()
            path.points.forEachIndexed { index, (x, yPos) ->
                // Map screen coords to PDF coords (very rough mapping for now)
                val pdfX = (x / context.resources.displayMetrics.widthPixels) * 595f
                val pdfY = (yPos / context.resources.displayMetrics.heightPixels) * 842f
                if (index == 0) androidPath.moveTo(pdfX, pdfY) else androidPath.lineTo(pdfX, pdfY)
            }
            canvas.drawPath(androidPath, paint)
        }
        
        pdfDocument.finishPage(page)
        
        val file = File(context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS), "PureWords_1611_${book}_$chapter.pdf")
        return try {
            pdfDocument.writeTo(FileOutputStream(file))
            pdfDocument.close()
            file
        } catch (_: Exception) {
            pdfDocument.close()
            null
        }
    }

    private fun breakText(text: String, maxWidth: Float, paint: Paint): List<String> {
        val words = text.split(" ")
        val lines = mutableListOf<String>()
        var currentLine = ""
        
        words.forEach { word ->
            val testLine = if (currentLine.isEmpty()) word else "$currentLine $word"
            currentLine = if (paint.measureText(testLine) <= maxWidth) {
                testLine
            } else {
                lines.add(currentLine)
                word
            }
        }
        if (currentLine.isNotEmpty()) lines.add(currentLine)
        return lines
    }
}
