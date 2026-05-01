package com.example.minitv.data.model
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "reports")
data class PlaybackReport(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,

    @ColumnInfo(name = "id_video") val idVideo: Int,
    @ColumnInfo(name = "video_name") val videoName: String,
    @ColumnInfo(name = "startTime") val startTime: Long
)