package com.example.minitv

import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.example.minitv.ui.viewmodel.MainViewModel
import com.example.minitv.ui.viewmodel.MainViewModelFactory
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class VideoPlayerActivity : AppCompatActivity(), SurfaceHolder.Callback {

    private lateinit var viewModel: MainViewModel
    private lateinit var surfaceView: SurfaceView
    private lateinit var playerControls: View
    private lateinit var playPauseButton: Button
    private var mediaPlayer: MediaPlayer? = null
    private var audioPlayer: MediaPlayer? = null
    private var isSurfaceReady = false
    private var currentVideoIdentifier: String? = null
    private var isPlaybackPaused = false
    private var areControlsVisible = true
    private val controlsHandler = Handler(Looper.getMainLooper())
    private val hideControlsRunnable = Runnable {
        hideControls()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContentView(R.layout.activity_video_player)
        hideSystemUi()

        surfaceView = findViewById(R.id.surfaceView)
        playerControls = findViewById(R.id.playerControls)
        surfaceView.holder.addCallback(this)
        playPauseButton = findViewById(R.id.playPauseButton)
        val previousButton: Button = findViewById(R.id.previousButton)
        val nextButton: Button = findViewById(R.id.nextButton)
        val rootView: View = findViewById(android.R.id.content)

        rootView.setOnClickListener {
            toggleControlsVisibility()
        }

        playPauseButton.setOnClickListener {
            togglePlayback()
            scheduleControlsAutoHide()
        }

        previousButton.setOnClickListener {
            viewModel.playPreviousVideo()
            scheduleControlsAutoHide()
        }

        nextButton.setOnClickListener {
            viewModel.playNextVideo()
            scheduleControlsAutoHide()
        }

        viewModel = ViewModelProvider(this, MainViewModelFactory(application))[MainViewModel::class.java]

        lifecycleScope.launch {
            viewModel.currentVideo.collectLatest { videoInfo ->
                videoInfo?.let {
                    currentVideoIdentifier = it.videoIdentifier
                    if (isSurfaceReady) {
                        playVideo(it.videoIdentifier)
                    }
                }
            }
        }
    }

    private fun hideSystemUi() {
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun playVideo(fileName: String) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer()
            releaseAudioPlayer()

            val assetFileDescriptor = assets.openFd("Videos/$fileName")
            mediaPlayer?.setDataSource(
                assetFileDescriptor.fileDescriptor,
                assetFileDescriptor.startOffset,
                assetFileDescriptor.length
            )
            assetFileDescriptor.close()

            mediaPlayer?.setDisplay(surfaceView.holder)

            mediaPlayer?.setOnPreparedListener { mp ->
                isPlaybackPaused = false
                updatePlayPauseButton()
                mp.start()
                playAudioForVideo(fileName)
                showControls()
                Log.d("MediaPlayer", "Playback started: $fileName")
            }

            mediaPlayer?.setOnCompletionListener {
                releaseAudioPlayer()
                Log.d("MediaPlayer", "Video completed, switching next...")
                viewModel.onVideoCompleted()
            }

            mediaPlayer?.setOnErrorListener { _, what, extra ->
                releaseAudioPlayer()
                Log.e("MediaPlayer", "Error: what=$what, extra=$extra")
                true
            }

            mediaPlayer?.prepareAsync()
        } catch (e: Exception) {
            Log.e("VideoPlayerActivity", "Failed to play video: ${e.message}")
        }
    }

    private fun togglePlayback() {
        val player = mediaPlayer ?: return

        if (isPlaybackPaused) {
            player.start()
            audioPlayer?.start()
            isPlaybackPaused = false
        } else {
            player.pause()
            if (audioPlayer?.isPlaying == true) {
                audioPlayer?.pause()
            }
            isPlaybackPaused = true
        }

        updatePlayPauseButton()
    }

    private fun updatePlayPauseButton() {
        val buttonTextRes = if (isPlaybackPaused) R.string.player_play else R.string.player_pause
        playPauseButton.text = getString(buttonTextRes)
    }

    private fun toggleControlsVisibility() {
        if (areControlsVisible) {
            hideControls()
        } else {
            showControls()
        }
    }

    private fun showControls() {
        playerControls.visibility = View.VISIBLE
        areControlsVisible = true
        scheduleControlsAutoHide()
    }

    private fun hideControls() {
        playerControls.visibility = View.GONE
        areControlsVisible = false
        controlsHandler.removeCallbacks(hideControlsRunnable)
    }

    private fun scheduleControlsAutoHide() {
        controlsHandler.removeCallbacks(hideControlsRunnable)
        controlsHandler.postDelayed(hideControlsRunnable, 3000L)
    }

    private fun playAudioForVideo(videoFileName: String) {
        val audioAssetPath = findAudioAssetPath(videoFileName)
        if (audioAssetPath == null) {
            Log.d("MediaPlayer", "Audio file not found for $videoFileName")
            return
        }

        try {
            audioPlayer = MediaPlayer()
            val audioDescriptor = assets.openFd(audioAssetPath)
            audioPlayer?.setDataSource(
                audioDescriptor.fileDescriptor,
                audioDescriptor.startOffset,
                audioDescriptor.length
            )
            audioDescriptor.close()
            audioPlayer?.setOnPreparedListener { it.start() }
            audioPlayer?.setOnErrorListener { _, what, extra ->
                Log.e("MediaPlayer", "Audio error: what=$what, extra=$extra")
                releaseAudioPlayer()
                true
            }
            audioPlayer?.prepareAsync()
            Log.d("MediaPlayer", "Audio started from: $audioAssetPath")
        } catch (e: Exception) {
            Log.e("VideoPlayerActivity", "Failed to play audio: ${e.message}")
            releaseAudioPlayer()
        }
    }

    private fun findAudioAssetPath(videoFileName: String): String? {
        val baseName = videoFileName.substringBeforeLast('.')
        val candidateFolders = listOf("Audio", "Audios", "Sounds", "Music")
        val candidateExtensions = listOf("mp3", "aac", "m4a", "wav", "ogg")
        val candidates = mutableListOf<String>()

        for (folder in candidateFolders) {
            for (extension in candidateExtensions) {
                candidates.add("$folder/$videoFileName.$extension")
                candidates.add("$folder/$baseName.$extension")
            }
        }

        for (candidatePath in candidates) {
            try {
                assets.openFd(candidatePath).close()
                return candidatePath
            } catch (_: Exception) {
            }
        }
        return null
    }

    private fun releaseAudioPlayer() {
        audioPlayer?.release()
        audioPlayer = null
    }

    override fun surfaceCreated(holder: SurfaceHolder) {
        isSurfaceReady = true
        currentVideoIdentifier?.let { playVideo(it) }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        isSurfaceReady = false
        mediaPlayer?.release()
        mediaPlayer = null
        releaseAudioPlayer()
    }

    override fun onDestroy() {
        super.onDestroy()
        controlsHandler.removeCallbacks(hideControlsRunnable)
        mediaPlayer?.release()
        mediaPlayer = null
        releaseAudioPlayer()
    }

    override fun onResume() {
        super.onResume()
        hideSystemUi()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            hideSystemUi()
        }
    }
}
