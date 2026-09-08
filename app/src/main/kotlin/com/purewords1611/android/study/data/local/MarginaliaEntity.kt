package com.purewords1611.android.study.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "marginalia")
data class MarginaliaEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val book: String,
    val chapter: Int,
    val drawingJson: String, // Serialized list of paths/points
    val updatedAtEpochMillis: Long = System.currentTimeMillis()
)
