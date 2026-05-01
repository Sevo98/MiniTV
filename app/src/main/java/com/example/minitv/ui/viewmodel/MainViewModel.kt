package com.example.minitv.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.minitv.data.model.VideoInfo
import com.example.minitv.data.repository.VideoRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch

class MainViewModel(
    application: Application,
    private val repository: VideoRepository
) : AndroidViewModel(application) {

    private val _currentVideo = kotlinx.coroutines.flow.MutableStateFlow<VideoInfo?>(null)
    val currentVideo: kotlinx.coroutines.flow.StateFlow<VideoInfo?> = _currentVideo

    val reportsFlow: Flow<List<com.example.minitv.data.model.PlaybackReport>> = repository.getReportsFlow()

    private val _videoList = mutableListOf<VideoInfo>()
    private var currentIndex = 0

    init {
        loadVideos()
    }

    private fun loadVideos() {
        viewModelScope.launch {
            try {
                val list = repository.getVideoList()
                _videoList.clear()
                _videoList.addAll(list)

                if (_videoList.isNotEmpty()) {
                    startVideo(0)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    fun onVideoCompleted() {
        currentIndex++

        if (currentIndex >= _videoList.size) {
            currentIndex = 0
        }

        startVideo(currentIndex)
    }

    private fun startVideo(index: Int) {
        if (index < 0 || index >= _videoList.size) return

        val videoInfo = _videoList[index]
        _currentVideo.value = videoInfo

        saveReport(videoInfo)
    }

    private fun saveReport(videoInfo: VideoInfo) {
        viewModelScope.launch {
            repository.savePlaybackReport(videoInfo)
        }
    }
}