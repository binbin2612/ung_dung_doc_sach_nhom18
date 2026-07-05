package com.example.ngdungocsach.user

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.example.ngdungocsach.R
import com.example.ngdungocsach.database.FirebaseHelper
import com.example.ngdungocsach.ui.BaseActivity
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SubscriptionActivity : BaseActivity() {

    private lateinit var firebaseHelper: FirebaseHelper
    private lateinit var txtSubscriptionStatus: TextView
    private lateinit var btnCancelSubscription: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subscription)

        firebaseHelper = FirebaseHelper()
        val sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val username = sharedPreferences.getString("username", null)
        val role = sharedPreferences.getString("role", null)

        if (role == "admin") {
            Toast.makeText(this, "Admin không thể sử dụng tính năng này", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val btnBack = findViewById<MaterialButton>(R.id.btnBack)
        val btnSubscribeMonth = findViewById<MaterialButton>(R.id.btnSubscribeMonth)
        val btnSubscribeYear = findViewById<MaterialButton>(R.id.btnSubscribeYear)
        txtSubscriptionStatus = findViewById(R.id.txtSubscriptionStatus)
        btnCancelSubscription = findViewById(R.id.btnCancelSubscription)

        updateSubscriptionUI(username)

        btnBack.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }

        btnSubscribeMonth.setOnClickListener {
            if (username != null) {
                firebaseHelper.updateSubscription(username, 30) { success ->
                    if (success) {
                        Toast.makeText(this, "Đăng ký Gói Tháng thành công (30 ngày)!", Toast.LENGTH_SHORT).show()
                        updateSubscriptionUI(username)
                    } else {
                        Toast.makeText(this, "Lỗi khi đăng ký gói", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        btnSubscribeYear.setOnClickListener {
            if (username != null) {
                firebaseHelper.updateSubscription(username, 365) { success ->
                    if (success) {
                        Toast.makeText(this, "Đăng ký Gói Năm thành công (365 ngày)!", Toast.LENGTH_SHORT).show()
                        updateSubscriptionUI(username)
                    } else {
                        Toast.makeText(this, "Lỗi khi đăng ký gói", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        btnCancelSubscription.setOnClickListener {
            if (username != null) {
                AlertDialog.Builder(this)
                    .setTitle("Hủy đăng ký")
                    .setMessage("Bạn có chắc chắn muốn hủy gói đăng ký hiện tại không?")
                    .setPositiveButton("Hủy gói") { _, _ ->
                        firebaseHelper.cancelSubscription(username) { success ->
                            if (success) {
                                Toast.makeText(this, "Đã hủy gói đăng ký", Toast.LENGTH_SHORT).show()
                                updateSubscriptionUI(username)
                            } else {
                                Toast.makeText(this, "Lỗi khi hủy gói", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                    .setNegativeButton("Quay lại", null)
                    .show()
            }
        }
    }

    private fun updateSubscriptionUI(username: String?) {
        if (username == null) {
            txtSubscriptionStatus.text = "Vui lòng đăng nhập để xem trạng thái"
            btnCancelSubscription.visibility = View.GONE
            return
        }

        firebaseHelper.getSubscriptionExpiry(username) { expiry ->
            val currentTime = System.currentTimeMillis()
            if (expiry > currentTime) {
                btnCancelSubscription.visibility = View.VISIBLE
                val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val dateStr = sdf.format(Date(expiry))
                
                val diff = expiry - currentTime
                val days = diff / (24 * 60 * 60 * 1000)
                
                if (days > 0) {
                    txtSubscriptionStatus.text = "Trạng thái: Đang hoạt động\nHạn dùng: $dateStr (Còn $days ngày)"
                } else {
                    val hours = diff / (60 * 60 * 1000)
                    txtSubscriptionStatus.text = "Trạng thái: Đang hoạt động\nHạn dùng: $dateStr (Còn $hours giờ)"
                }
            } else {
                btnCancelSubscription.visibility = View.GONE
                txtSubscriptionStatus.text = "Trạng thái: Chưa đăng ký hoặc đã hết hạn"
            }
        }
    }
}
