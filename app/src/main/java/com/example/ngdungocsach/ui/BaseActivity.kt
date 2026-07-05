package com.example.ngdungocsach.ui

import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.example.ngdungocsach.R

open class BaseActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun startActivity(intent: Intent?) {
        super.startActivity(intent)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, R.anim.slide_in_right, R.anim.slide_out_left)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        }
    }

    override fun finish() {
        super.finish()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_CLOSE, R.anim.slide_in_left, R.anim.slide_out_right)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
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
