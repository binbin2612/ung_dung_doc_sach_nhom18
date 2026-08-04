package com.example.ngdungocsach.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.Toast
import com.example.ngdungocsach.R
import com.example.ngdungocsach.database.FirebaseHelper
import com.example.ngdungocsach.ui.BaseActivity
import com.example.ngdungocsach.user.MainActivity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import java.util.concurrent.TimeUnit

class PhoneLoginActivity : BaseActivity() {

    private lateinit var firebaseHelper: FirebaseHelper
    private var verificationId: String? = null
    private lateinit var phoneNumber: String

    // Note: I'm not using view binding in this project usually, but let's stick to findViewById for consistency if needed.
    // Actually the user project seems to use findViewById.
    
    private lateinit var edtPhone: android.widget.EditText
    private lateinit var edtOtp: android.widget.EditText
    private lateinit var btnAction: com.google.android.material.button.MaterialButton
    private lateinit var tilOtp: com.google.android.material.textfield.TextInputLayout
    private lateinit var tvInstruction: android.widget.TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_phone_login)

        firebaseHelper = FirebaseHelper()

        edtPhone = findViewById(R.id.edtPhone)
        edtOtp = findViewById(R.id.edtOtp)
        btnAction = findViewById(R.id.btnAction)
        tilOtp = findViewById(R.id.tilOtp)
        tvInstruction = findViewById(R.id.tvInstruction)
        val btnBack = findViewById<com.google.android.material.button.MaterialButton>(R.id.btnBack)

        btnBack.setOnClickListener { finish() }

        btnAction.setOnClickListener {
            if (verificationId == null) {
                phoneNumber = edtPhone.text.toString().trim()
                if (phoneNumber.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập số điện thoại", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                sendVerificationCode(phoneNumber)
            } else {
                val code = edtOtp.text.toString().trim()
                if (code.isEmpty()) {
                    Toast.makeText(this, "Vui lòng nhập mã OTP", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                verifyCode(code)
            }
        }
    }

    private fun sendVerificationCode(number: String) {
        val options = PhoneAuthOptions.newBuilder(com.google.firebase.auth.FirebaseAuth.getInstance())
            .setPhoneNumber(number)
            .setTimeout(60L, TimeUnit.SECONDS)
            .setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    signInWithPhone(credential)
                }

                override fun onVerificationFailed(e: FirebaseException) {
                    Toast.makeText(this@PhoneLoginActivity, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
                }

                override fun onCodeSent(id: String, token: PhoneAuthProvider.ForceResendingToken) {
                    verificationId = id
                    tilOtp.visibility = View.VISIBLE
                    btnAction.text = "Xác nhận OTP"
                    tvInstruction.text = "Mã OTP đã được gửi đến $number"
                    Toast.makeText(this@PhoneLoginActivity, "Đã gửi mã OTP", Toast.LENGTH_SHORT).show()
                }
            })
            .build()
        PhoneAuthProvider.verifyPhoneNumber(options)
    }

    private fun verifyCode(code: String) {
        val credential = PhoneAuthProvider.getCredential(verificationId!!, code)
        signInWithPhone(credential)
    }

    private fun signInWithPhone(credential: PhoneAuthCredential) {
        firebaseHelper.signInWithCredential(credential) { success, message ->
            if (success) {
                val user = firebaseHelper.getCurrentUser()
                user?.let {
                    firebaseHelper.getUserData(it.uid) { userData ->
                        val sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
                        val editor = sharedPreferences.edit()
                        editor.putString("username", userData?.username ?: it.phoneNumber)
                        editor.putString("role", userData?.role ?: "user")
                        editor.apply()

                        Toast.makeText(this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish()
                    }
                }
            } else {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}
