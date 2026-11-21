package com.example.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface SentenceAnalysisDao {
    @Query("SELECT * FROM sentence_analyses ORDER BY isPinned DESC, createdAt DESC")
    fun getAllAnalyses(): Flow<List<SentenceAnalysisEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAnalysis(entity: SentenceAnalysisEntity): Long

    @Query("UPDATE sentence_analyses SET isPinned = :pinned WHERE id = :id")
    suspend fun updatePinned(id: Long, pinned: Boolean)

    @Query("DELETE FROM sentence_analyses WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM sentence_analyses")
    suspend fun deleteAll()
}
