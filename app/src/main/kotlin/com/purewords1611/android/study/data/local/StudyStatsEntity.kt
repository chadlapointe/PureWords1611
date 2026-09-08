package com.purewords1611.android.study.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "study_stats")
data class StudyStatsEntity(
    @PrimaryKey val date: String, // "YYYY-MM-DD"
    val chaptersCompleted: Int = 0,
    val versesRead: Int = 0,
    val minutesSpent: Int = 0,
    val pointsEarned: Int = 0
)

data class StreakInfo(
    val currentStreak: Int,
    val longestStreak: Int,
    val totalChapters: Int,
    val lastActiveDate: String?
)
