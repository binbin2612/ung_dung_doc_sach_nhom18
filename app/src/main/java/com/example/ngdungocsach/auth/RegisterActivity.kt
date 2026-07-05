package com.example.ngdungocsach.auth

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.Toast
import com.example.ngdungocsach.R
import com.example.ngdungocsach.database.FirebaseHelper
import com.example.ngdungocsach.model.User
import com.example.ngdungocsach.user.MainActivity
import com.example.ngdungocsach.ui.BaseActivity
import com.google.android.material.button.MaterialButton

class RegisterActivity : BaseActivity() {

    private lateinit var firebaseHelper: FirebaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        firebaseHelper = FirebaseHelper()

        val edtUser = findViewById<EditText>(R.id.edtNewUsername)
        val edtPass = findViewById<EditText>(R.id.edtNewPassword)
        val btnRegister = findViewById<MaterialButton>(R.id.btnRegister)
        val btnBack = findViewById<MaterialButton>(R.id.btnBack)
        val btnBackToLogin = findViewById<MaterialButton>(R.id.btnBackToLogin)

        btnBack.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        btnBackToLogin.setOnClickListener {
            finish()
        }

        btnRegister.setOnClickListener {
            val username = edtUser.text.toString().trim()
            val password = edtPass.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Chuyển sang dùng FirebaseHelper
            firebaseHelper.checkUserExists(username) { exists ->
                if (exists) {
                    Toast.makeText(this, "Tên đăng nhập đã tồn tại", Toast.LENGTH_SHORT).show()
                } else {
                    val newUser = User(username = username, role = "user", subscriptionExpiry = 0)
                    firebaseHelper.register(newUser, password) { success ->
                        if (success) {
                            Toast.makeText(this, "Đăng ký thành công", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            Toast.makeText(this, "Đăng ký thất bại, vui lòng thử lại", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }
        }
    }
}
