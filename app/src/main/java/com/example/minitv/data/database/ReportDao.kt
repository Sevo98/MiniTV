package com.example.minitv.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.minitv.data.model.PlaybackReport
import kotlinx.coroutines.flow.Flow

@Dao
interface ReportDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReport(report: PlaybackReport): Long

    @Query("SELECT * FROM reports ORDER BY startTime DESC")
    fun getAllReportsFlow(): Flow<List<PlaybackReport>>
}