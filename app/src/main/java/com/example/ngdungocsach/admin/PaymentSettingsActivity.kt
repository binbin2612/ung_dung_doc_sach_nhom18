package com.example.ngdungocsach.admin

import android.os.Bundle
import android.widget.Toast
import com.example.ngdungocsach.R
import com.example.ngdungocsach.database.FirebaseHelper
import com.example.ngdungocsach.ui.BaseActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class PaymentSettingsActivity : BaseActivity() {

    private lateinit var firebaseHelper: FirebaseHelper
    private lateinit var etMomoQr: TextInputEditText
    private lateinit var etBankQr: TextInputEditText
    private lateinit var btnSave: MaterialButton
    private lateinit var btnBack: MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_payment_settings)

        firebaseHelper = FirebaseHelper()
        etMomoQr = findViewById(R.id.etMomoQr)
        etBankQr = findViewById(R.id.etBankQr)
        btnSave = findViewById(R.id.btnSave)
        btnBack = findViewById(R.id.btnBack)

        // Tải dữ liệu hiện tại
        firebaseHelper.getPaymentSettings { settings ->
            if (settings != null) {
                etMomoQr.setText(settings["momo_qr"]?.toString() ?: "")
                etBankQr.setText(settings["bank_qr"]?.toString() ?: "")
            }
        }

        btnBack.setOnClickListener { finish() }

        btnSave.setOnClickListener {
            val momoQr = etMomoQr.text.toString()
            val bankQr = etBankQr.text.toString()

            val settings = mapOf(
                "momo_qr" to momoQr,
                "bank_qr" to bankQr
            )

            firebaseHelper.updatePaymentSettings(settings) { success ->
                if (success) {
                    Toast.makeText(this, "Đã cập nhật cấu hình thanh toán", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Lỗi khi lưu cấu hình", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
