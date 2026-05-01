package com.example.minitv.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.minitv.data.database.AppDatabase
import com.example.minitv.data.repository.VideoRepository

class MainViewModelFactory(
    private val application: Application
) : ViewModelProvider.Factory {

    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            val db = AppDatabase.getDatabase(application.applicationContext)
            val repository = VideoRepository(application.applicationContext, db.reportDao())
            return MainViewModel(application, repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}