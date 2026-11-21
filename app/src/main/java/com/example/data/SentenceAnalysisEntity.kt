package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sentence_analyses")
data class SentenceAnalysisEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val sentence: String,
    val sentenceType: String,
    val tokensJson: String,
    val createdAt: Long = System.currentTimeMillis(),
    val isPinned: Boolean = false,
    val layer2Used: Boolean = false
)
