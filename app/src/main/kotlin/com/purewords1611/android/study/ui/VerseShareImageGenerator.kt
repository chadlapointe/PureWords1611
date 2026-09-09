package com.purewords1611.android.study.ui

import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.purewords1611.android.R
import java.io.File
import java.io.FileOutputStream

object VerseShareImageGenerator {

    fun generateAndShareVerseImage(
        context: Context,
        verseText: String,
        citation: String,
        translationName: String
    ) {
        val bitmap = createVerseBitmap(context, verseText, citation, translationName)
        val file = saveBitmapToCache(context, bitmap)
        if (file != null) {
            shareImageFile(context, file, "$citation ($translationName)")
        }
    }

    fun createVerseBitmap(
        context: Context,
        verseText: String,
        citation: String,
        translationName: String
    ): Bitmap {
        val width = 1080
        val height = 1080
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)

        // Parchment Background
        canvas.drawColor(Color.parseColor("#F4ECD8"))

        // Border Lines
        val borderPaint = Paint().apply {
            color = Color.parseColor("#3E2723")
            style = Paint.Style.STROKE
            strokeWidth = 3f
            isAntiAlias = true
        }

        canvas.drawRect(RectF(40f, 40f, width - 40f, height - 40f), borderPaint)
        canvas.drawRect(RectF(50f, 50f, width - 50f, height - 50f), borderPaint)

        // Header Paint
        val headerPaint = TextPaint().apply {
            color = Color.parseColor("#5D4037")
            textSize = 32f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD)
        }

        canvas.drawText("PURE WORDS 1611 BIBLE", width / 2f, 110f, headerPaint)

        // Blackletter Font for Verse Text
        val blackletterTypeface = try {
            ResourcesCompat.getFont(context, R.font.kjva6aa) ?: Typeface.SERIF
        } catch (_: Exception) {
            Typeface.SERIF
        }

        val versePaint = TextPaint().apply {
            color = Color.parseColor("#1B1B1B")
            textSize = 48f
            isAntiAlias = true
            typeface = blackletterTypeface
        }

        // Format multi-line verse text using StaticLayout
        val formattedVerseText = "“$verseText”"
        val textWidth = width - 180
        val staticLayout = StaticLayout.Builder.obtain(
            formattedVerseText,
            0,
            formattedVerseText.length,
            versePaint,
            textWidth
        )
            .setAlignment(Layout.Alignment.ALIGN_CENTER)
            .setLineSpacing(8f, 1.2f)
            .build()

        val textHeight = staticLayout.height
        val startY = (height - textHeight) / 2f

        canvas.save()
        canvas.translate(90f, startY)
        staticLayout.draw(canvas)
        canvas.restore()

        // Citation & Footer
        val citationPaint = TextPaint().apply {
            color = Color.parseColor("#3E2723")
            textSize = 38f
            isAntiAlias = true
            textAlign = Paint.Align.CENTER
            typeface = Typeface.create(Typeface.SERIF, Typeface.BOLD_ITALIC)
        }

        canvas.drawText("— $citation ($translationName)", width / 2f, height - 120f, citationPaint)

        return bitmap
    }

    private fun saveBitmapToCache(context: Context, bitmap: Bitmap): File? {
        return try {
            val shareDir = File(context.cacheDir, "shares").apply { mkdirs() }
            val file = File(shareDir, "verse_share.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            file
        } catch (_: Exception) {
            null
        }
    }

    private fun shareImageFile(context: Context, file: File, title: String) {
        try {
            val uri = FileProvider.getUriForFile(
                context,
                "${context.packageName}.fileprovider",
                file
            )

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                putExtra(Intent.EXTRA_SUBJECT, title)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }

            val chooser = Intent.createChooser(intent, "Share Verse Image")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (_: Exception) {
            // Fallback text sharing if image provider fails
        }
    }
}
