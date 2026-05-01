package com.example.minitv

import android.media.MediaPlayer
import android.os.Bundle
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.minitv.ui.viewmodel.MainViewModel
import com.example.minitv.ui.viewmodel.MainViewModelFactory
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.collectLatest
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity(), SurfaceHolder.Callback {

    private lateinit var viewModel: MainViewModel
    private lateinit var surfaceView: SurfaceView
    private var mediaPlayer: MediaPlayer? = null
    private var isSurfaceReady = false
    private var currentVideoIdentifier: String? = null

    private val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        surfaceView = findViewById(R.id.surfaceView)
        surfaceView.holder.addCallback(this)

        // Инициализация ViewModel через Factory
        viewModel = ViewModelProvider(this, MainViewModelFactory(application))[MainViewModel::class.java]

        // 🔹 Подписка на поток текущего видео (запуск воспроизведения)
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

        // 🔹 НОВАЯ ПОДПИСКА: Логирование записей в БД (пункт F)
        lifecycleScope.launch {
            repeatOnLifecycle(androidx.lifecycle.Lifecycle.State.STARTED) {
                viewModel.reportsFlow.collectLatest { reports ->
                    Log.d("DB_Verify", "Total reports in DB: ${reports.size}")

                    if (reports.isNotEmpty()) {
                        // Берём самый свежий отчёт (если в DAO есть ORDER BY startTime DESC)
                        val latest = reports.firstOrNull()
                        latest?.let {
                            val formattedTime = dateFormat.format(Date(it.startTime))
                            Log.d("DB_Verify", "Latest: id_video=${it.idVideo} | name=${it.videoName} | started=$formattedTime")
                        }
                    }
                }
            }
        }
    }

    /**
     * Настройка и запуск MediaPlayer
     */
    private fun playVideo(fileName: String) {
        try {
            mediaPlayer?.release()
            mediaPlayer = MediaPlayer()

            val assetFileDescriptor = assets.openFd("Videos/$fileName")
            mediaPlayer?.setDataSource(
                assetFileDescriptor.fileDescriptor,
                assetFileDescriptor.startOffset,
                assetFileDescriptor.length
            )
            assetFileDescriptor.close()

            mediaPlayer?.setDisplay(surfaceView.holder)

            mediaPlayer?.setOnPreparedListener { mp ->
                mp.start()
                Log.d("MediaPlayer", "Playback started: $fileName")
            }

            mediaPlayer?.setOnCompletionListener {
                Log.d("MediaPlayer", "Video completed, switching next...")
                viewModel.onVideoCompleted()
            }

            mediaPlayer?.setOnErrorListener { _, what, extra ->
                Log.e("MediaPlayer", "Error: what=$what, extra=$extra")
                true
            }

            mediaPlayer?.prepareAsync()

        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to play video: ${e.message}")
        }
    }

    //region SurfaceHolder.Callback
    override fun surfaceCreated(holder: SurfaceHolder) {
        isSurfaceReady = true
        currentVideoIdentifier?.let { playVideo(it) }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        isSurfaceReady = false
        mediaPlayer?.release()
        mediaPlayer = null
    }
    //endregion

    override fun onDestroy() {
        super.onDestroy()
        mediaPlayer?.release()
        mediaPlayer = null
    }
}