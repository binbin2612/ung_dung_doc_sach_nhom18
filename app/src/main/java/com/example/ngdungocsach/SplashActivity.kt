package com.example.ngdungocsach

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.ngdungocsach.user.MainActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val imgLogo = findViewById<ImageView>(R.id.imgLogo)
        val txtAppName = findViewById<TextView>(R.id.txtAppName)
        val txtSlogan = findViewById<TextView>(R.id.txtSlogan)

        // 1. Thiết lập trạng thái ẩn ban đầu
        imgLogo.alpha = 0f
        imgLogo.scaleX = 0.3f
        imgLogo.scaleY = 0.3f
        
        txtAppName.alpha = 0f
        txtAppName.translationY = 50f
        
        txtSlogan.alpha = 0f

        // 2. Chạy chuỗi hiệu ứng (Sequential Animation)
        imgLogo.post {
            // Bước 1: Logo phóng to và hiện ra
            imgLogo.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .setDuration(600)
                .setInterpolator(DecelerateInterpolator())
                .withEndAction {
                    // Bước 2: Tên ứng dụng bay lên
                    txtAppName.animate()
                        .alpha(1f)
                        .translationY(0f)
                        .setDuration(400)
                        .setInterpolator(DecelerateInterpolator())
                        .withEndAction {
                            // Bước 3: Slogan mờ dần vào
                            txtSlogan.animate()
                                .alpha(1f)
                                .setDuration(400)
                                .start()
                        }
                        .start()
                }
                .start()
        }

        // 3. Chuyển sang màn hình chính sau khi hoàn tất animation
        Handler(Looper.getMainLooper()).postDelayed({
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            
            // Xử lý chuyển cảnh cho các phiên bản Android khác nhau
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, android.R.anim.fade_in, android.R.anim.fade_out)
            } else {
                @Suppress("DEPRECATION")
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
            }
            
            finish()
        }, 2000)
    }
}
