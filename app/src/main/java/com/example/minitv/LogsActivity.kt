package com.example.minitv

import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.example.minitv.data.database.AppDatabase
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class LogsActivity : AppCompatActivity() {

    private lateinit var logsTextView: TextView
    private val dateFormat = SimpleDateFormat("dd-MM-yyyy HH:mm:ss", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_logs)

        logsTextView = findViewById(R.id.tvLogs)
        val reportDao = AppDatabase.getDatabase(applicationContext).reportDao()

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                reportDao.getAllReportsFlow().collectLatest { reports ->
                    logsTextView.text = if (reports.isEmpty()) {
                        getString(R.string.logs_empty)
                    } else {
                        reports.joinToString(separator = "\n\n") { report ->
                            "id=${report.id}\n" +
                                "videoId=${report.idVideo}\n" +
                                "videoName=${report.videoName}\n" +
                                "started=${dateFormat.format(Date(report.startTime))}"
                        }
                    }
                }
            }
        }
    }
}
