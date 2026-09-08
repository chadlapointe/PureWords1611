package com.purewords1611.android.study.nudge

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.purewords1611.android.MainActivity
import com.purewords1611.android.R
import com.purewords1611.android.study.data.ExplanationDepth
import com.purewords1611.android.study.data.StudyRepository
import com.purewords1611.android.study.data.local.ChapterCompletionEntity
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

@Singleton
class StudyNudgeManager @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val repository: StudyRepository,
) {
    private val notificationManager = NotificationManagerCompat.from(context)

    companion object {
        private const val CHANNEL_ID = "study_nudges"
        private const val NOTIFICATION_ID = 1611
    }

    init {
        createNotificationChannel()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = "Bible Study Nudges"
            val descriptionText = "Periodic reminders and questions about your recent study."
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {
                description = descriptionText
            }
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    suspend fun sendNudge(completion: ChapterCompletionEntity) {
        val verses = repository.getChapterVerses(completion.book, completion.chapter)
        if (verses.isEmpty()) return

        // Try to find any verse in this chapter that has explanations
        var question: String? = null
        
        // Shuffle verses to find a random one with explanations
        for (verse in verses.shuffled()) {
            val explanations = repository.observeExplanations(verse.id, ExplanationDepth.HISTORICAL_LINGUISTIC).first()
            if (explanations.isNotEmpty()) {
                val explanation = explanations.random()
                question = "Recall the study of '${completion.book} ${completion.chapter}:${verse.verse}': ${explanation.contentMarkdown.take(100)}..."
                break
            }
        }

        val finalQuestion = question ?: "You recently finished ${completion.book} ${completion.chapter}. Take a moment to reflect on what you've learned."

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            putExtra("book", completion.book)
            putExtra("chapter", completion.chapter)
        }
        
        val pendingIntent = PendingIntent.getActivity(
            context, 
            0, 
            intent, 
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("${completion.book} ${completion.chapter} Reflection")
            .setContentText(finalQuestion)
            .setStyle(NotificationCompat.BigTextStyle().bigText(finalQuestion))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            notificationManager.notify(NOTIFICATION_ID, builder.build())
        } catch (ignored: SecurityException) {
            // Permission not granted
        }
    }
}
