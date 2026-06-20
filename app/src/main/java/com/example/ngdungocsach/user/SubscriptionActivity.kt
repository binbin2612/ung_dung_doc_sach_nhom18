package com.example.ngdungocsach.user

import android.app.AlertDialog
import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.TextView
import android.widget.Toast
import com.example.ngdungocsach.R
import com.example.ngdungocsach.database.DatabaseHelper
import com.example.ngdungocsach.ui.BaseActivity
import com.google.android.material.button.MaterialButton
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SubscriptionActivity : BaseActivity() {

    private lateinit var db: DatabaseHelper
    private lateinit var txtSubscriptionStatus: TextView
    private lateinit var btnCancelSubscription: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_subscription)

        db = DatabaseHelper(this)
         val sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val username = sharedPreferences.getString("username", null)

        val btnBack = findViewById<MaterialButton>(R.id.btnBack)
        val btnSubscribeMonth = findViewById<MaterialButton>(R.id.btnSubscribeMonth)
        val btnSubscribeYear = findViewById<MaterialButton>(R.id.btnSubscribeYear)
        txtSubscriptionStatus = findViewById(R.id.txtSubscriptionStatus)
        btnCancelSubscription = findViewById(R.id.btnCancelSubscription)

        updateSubscriptionUI(username)

        btnBack.setOnClickListener {
            finish()
        }

        btnSubscribeMonth.setOnClickListener {
            if (username != null) {
                if (db.updateSubscription(username, 30)) {
                    Toast.makeText(this, "Đăng ký Gói Tháng thành công (30 ngày)!", Toast.LENGTH_SHORT).show()
                    updateSubscriptionUI(username)
                } else {
                    Toast.makeText(this, "Lỗi khi đăng ký gói", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnSubscribeYear.setOnClickListener {
            if (username != null) {
                if (db.updateSubscription(username, 365)) {
                    Toast.makeText(this, "Đăng ký Gói Năm thành công (365 ngày)!", Toast.LENGTH_SHORT).show()
                    updateSubscriptionUI(username)
                } else {
                    Toast.makeText(this, "Lỗi khi đăng ký gói", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnCancelSubscription.setOnClickListener {
            if (username != null) {
                AlertDialog.Builder(this)
                    .setTitle("Hủy đăng ký")
                    .setMessage("Bạn có chắc chắn muốn hủy gói đăng ký hiện tại không?")
                    .setPositiveButton("Hủy gói") { _, _ ->
                        if (db.cancelSubscription(username)) {
                            Toast.makeText(this, "Đã hủy gói thành công", Toast.LENGTH_SHORT).show()
                            updateSubscriptionUI(username)
                        } else {
                            Toast.makeText(this, "Lỗi khi hủy gói", Toast.LENGTH_SHORT).show()
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

        val expiry = db.getSubscriptionExpiry(username)
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
