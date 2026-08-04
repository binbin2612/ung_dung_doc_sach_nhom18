package com.example.ngdungocsach.user

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import com.example.ngdungocsach.R
import com.example.ngdungocsach.database.FirebaseHelper
import com.example.ngdungocsach.ui.BaseActivity
import com.google.android.material.button.MaterialButton
import com.bumptech.glide.Glide
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
        val uid = sharedPreferences.getString("uid", null)
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

        updateSubscriptionUI(uid)

        btnBack.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }

        btnSubscribeMonth.setOnClickListener {
            if (uid != null && username != null) {
                showPaymentDialog(uid, username, "Gói Premium Tháng", 1000, 30)
            }
        }

        btnSubscribeYear.setOnClickListener {
            if (uid != null && username != null) {
                showPaymentDialog(uid, username, "Gói Premium Năm", 5000, 365)
            }
        }

        btnCancelSubscription.setOnClickListener {
            if (uid != null) {
                AlertDialog.Builder(this)
                    .setTitle("Hủy đăng ký")
                    .setMessage("Bạn có chắc chắn muốn hủy gói đăng ký hiện tại không?")
                    .setPositiveButton("Hủy gói") { _, _ ->
                        firebaseHelper.cancelSubscription(uid) { success ->
                            if (success) {
                                Toast.makeText(this, "Đã hủy gói đăng ký", Toast.LENGTH_SHORT).show()
                                updateSubscriptionUI(uid)
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

    private fun showPaymentDialog(uid: String, username: String, packageName: String, amount: Long, days: Int) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_payment_qr, null)
        val tvAmount = dialogView.findViewById<TextView>(R.id.tvPaymentAmount)
        val tvPackage = dialogView.findViewById<TextView>(R.id.tvPaymentPackage)
        val imgQr = dialogView.findViewById<ImageView>(R.id.imgPaymentQr)
        val btnBank = dialogView.findViewById<MaterialButton>(R.id.btnPayBank)
        val btnMomo = dialogView.findViewById<MaterialButton>(R.id.btnPayMomo)
        val btnConfirm = dialogView.findViewById<MaterialButton>(R.id.btnConfirmPaid)

        tvAmount.text = "Số tiền: %,d VNĐ".format(amount)
        tvPackage.text = "Gói: $packageName"

        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        firebaseHelper.getPaymentSettings { settings ->
            val customMomoQr = settings?.get("momo_qr")?.toString()
            val customBankQr = settings?.get("bank_qr")?.toString()

            btnBank.setOnClickListener {
                val qrUrl = if (!customBankQr.isNullOrBlank()) {
                    customBankQr
                } else {
                    "https://api.qrserver.com/v1/create-qr-code/?size=400x400&data=BANK_PAYMENT_FOR_${username}_PACKAGE_${packageName}"
                }
                Glide.with(this).load(qrUrl).placeholder(R.drawable.white).into(imgQr)
                Toast.makeText(this, "Đã chọn Ngân hàng. Vui lòng quét mã QR bên dưới.", Toast.LENGTH_SHORT).show()
                imgQr.visibility = View.VISIBLE
                btnConfirm.visibility = View.VISIBLE
            }

            btnMomo.setOnClickListener {
                val qrUrl = if (!customMomoQr.isNullOrBlank()) {
                    customMomoQr
                } else {
                    "https://api.qrserver.com/v1/create-qr-code/?size=400x400&data=MOMO_PAYMENT_FOR_${username}_PACKAGE_${packageName}"
                }
                Glide.with(this).load(qrUrl).placeholder(R.drawable.white).into(imgQr)
                Toast.makeText(this, "Đã chọn MoMo. Vui lòng quét mã QR bên dưới.", Toast.LENGTH_SHORT).show()
                imgQr.visibility = View.VISIBLE
                btnConfirm.visibility = View.VISIBLE
            }
        }

        btnConfirm.setOnClickListener {
            firebaseHelper.updateSubscription(uid, days) { success ->
                if (success) {
                    val payment = com.example.ngdungocsach.model.Payment(
                        username = username,
                        packageName = packageName,
                        amount = amount,
                        timestamp = System.currentTimeMillis()
                    )
                    firebaseHelper.savePayment(payment) { _ ->
                        Toast.makeText(this, "Thanh toán thành công! Gói của bạn đã được gia hạn.", Toast.LENGTH_LONG).show()
                        updateSubscriptionUI(uid)
                        dialog.dismiss()
                    }
                } else {
                    Toast.makeText(this, "Lỗi khi cập nhật gói", Toast.LENGTH_SHORT).show()
                }
            }
        }

        dialog.show()
    }

    private fun updateSubscriptionUI(uid: String?) {
        if (uid == null) {
            txtSubscriptionStatus.text = "Vui lòng đăng nhập để xem trạng thái"
            btnCancelSubscription.visibility = View.GONE
            return
        }

        firebaseHelper.getSubscriptionExpiry(uid) { expiry ->
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
