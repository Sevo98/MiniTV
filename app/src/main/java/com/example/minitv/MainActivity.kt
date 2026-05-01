package com.example.minitv

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val openVideoButton: Button = findViewById(R.id.btnOpenVideo)
        val openLogsButton: Button = findViewById(R.id.btnOpenLogs)

        openVideoButton.setOnClickListener {
            startActivity(Intent(this, VideoPlayerActivity::class.java))
        }

        openLogsButton.setOnClickListener {
            startActivity(Intent(this, LogsActivity::class.java))
        }
    }
}