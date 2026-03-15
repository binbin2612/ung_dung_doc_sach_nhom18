package com.example.ngdungocsach.ui

import android.content.Context
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        val sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        
        // Áp dụng Theme
        val themeMode = sharedPreferences.getInt("theme_mode", 2) // Mặc định là Theo hệ thống (2)
        when (themeMode) {
            0 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            2 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
        }

        super.onCreate(savedInstanceState)
    }

    override fun attachBaseContext(newBase: Context) {
        val sharedPreferences = newBase.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val fontSizeIndex = sharedPreferences.getInt("font_size", 1) // Mặc định là Trung bình (1)
        
        val scale = when (fontSizeIndex) {
            0 -> 0.85f // Nhỏ
            1 -> 1.0f  // Trung bình
            2 -> 1.15f // Lớn
            else -> 1.0f
        }

        val configuration = newBase.resources.configuration
        configuration.fontScale = scale
        val context = newBase.createConfigurationContext(configuration)
        super.attachBaseContext(context)
    }
}
