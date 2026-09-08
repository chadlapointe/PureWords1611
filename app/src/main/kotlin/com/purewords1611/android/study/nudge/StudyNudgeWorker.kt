package com.purewords1611.android.study.nudge

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.purewords1611.android.study.data.StudyRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

@HiltWorker
class StudyNudgeWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted params: WorkerParameters,
    private val repository: StudyRepository,
    private val nudgeManager: StudyNudgeManager
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        // Only nudge if the user hasn't been nudged in the last 4 hours
        val fourHoursAgo = System.currentTimeMillis() - TimeUnit.HOURS.toMillis(4)
        
        val pendingCompletion = repository.getOldestPendingQuestion(fourHoursAgo)
        
        if (pendingCompletion != null) {
            nudgeManager.sendNudge(pendingCompletion)
            repository.updateLastQuestionTime(pendingCompletion.id, System.currentTimeMillis())
        }

        return Result.success()
    }
}
