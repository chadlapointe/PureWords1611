package com.purewords1611.android

import android.app.Application
import androidx.core.content.edit
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.work.Constraints
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.firebase.FirebaseApp
import com.purewords1611.android.study.nudge.StudyNudgeWorker
import dagger.hilt.android.HiltAndroidApp
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Application class for PureWords1611
 * Handles app-wide initialization including Firebase and Analytics
 * 
 * @HiltAndroidApp triggers Hilt's code generation for dependency injection
 */
@HiltAndroidApp
class PureWordsApplication : Application(), Configuration.Provider {
    
    @Inject
    lateinit var workerFactory: HiltWorkerFactory

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()
    
    override fun onCreate() {
        super.onCreate()
        
        // Initialize Firebase
        FirebaseApp.initializeApp(this)

        checkDatabaseVersion()
        scheduleStudyNudges()
    }

    private fun checkDatabaseVersion() {
        val currentDbVersion = 48
        val prefs = getSharedPreferences("db_prefs", MODE_PRIVATE)
        val lastResetVersion = prefs.getInt("last_reset_version", 0)
        
        if (lastResetVersion < currentDbVersion) {
            val dbName = "pure_words_study.db"
            val dbFile = getDatabasePath(dbName)
            if (dbFile.exists()) {
                android.util.Log.i("PureWordsApplication", "Database version mismatch. Deleting old DB to force recopy.")
                dbFile.delete()
                File("${dbFile.path}-wal").delete()
                File("${dbFile.path}-shm").delete()
                File("${dbFile.path}-journal").delete()
            }
            prefs.edit { putInt("last_reset_version", currentDbVersion) }
        }
    }

    private fun scheduleStudyNudges() {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.NOT_REQUIRED)
            .setRequiresBatteryNotLow(true)
            .build()

        val nudgeRequest = PeriodicWorkRequestBuilder<StudyNudgeWorker>(4, TimeUnit.HOURS)
            .setConstraints(constraints)
            .addTag("study_nudges")
            .build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "study_nudges_periodic",
            androidx.work.ExistingPeriodicWorkPolicy.KEEP,
            nudgeRequest
        )
    }
}
