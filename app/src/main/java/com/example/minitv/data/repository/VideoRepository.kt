package com.example.minitv.data.repository

import android.content.Context
import android.util.Log
import com.example.minitv.data.database.ReportDao
import com.example.minitv.data.model.PlaybackReport
import com.example.minitv.data.model.VideoInfo
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.*

class VideoRepository(
    private val context: Context,
    private val reportDao: ReportDao
) {
    private val gson = Gson()
    private val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())

    suspend fun getVideoList(): List<VideoInfo> = withContext(Dispatchers.IO) {
        val inputStream = context.assets.open("medialist.json")
        val jsonContent = inputStream.bufferedReader().use { it.readText() }
        val type = object : TypeToken<List<VideoInfo>>() {}.type
        gson.fromJson(jsonContent, type)
    }

    suspend fun savePlaybackReport(videoInfo: VideoInfo) {
        val report = PlaybackReport(
            idVideo = videoInfo.videoId,
            videoName = videoInfo.videoIdentifier,
            startTime = System.currentTimeMillis()
        )

        val rowId = reportDao.insertReport(report)

        Log.d("DB_Repository", "Report saved | ID: $rowId | Video: ${report.videoName} | Time: ${dateFormat.format(Date(report.startTime))}")
    }

    fun getReportsFlow(): Flow<List<PlaybackReport>> = reportDao.getAllReportsFlow()
}