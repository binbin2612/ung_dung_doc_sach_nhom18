package com.example.ngdungocsach.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import com.example.ngdungocsach.database.FirebaseHelper
import com.example.ngdungocsach.admin.AdminActivity
import com.example.ngdungocsach.user.MainActivity
import com.example.ngdungocsach.R
import com.example.ngdungocsach.ui.BaseActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class LoginActivity : BaseActivity() {

    private lateinit var firebaseHelper: FirebaseHelper
    private lateinit var sharedPreferences: android.content.SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        firebaseHelper = FirebaseHelper()
        sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)

        val btnBack = findViewById<MaterialButton>(R.id.btnBack)
        val layoutLogin = findViewById<LinearLayout>(R.id.layoutLogin)
        val layoutUserInfo = findViewById<ScrollView>(R.id.layoutUserInfo)

        // Login views
        val txtUser = findViewById<EditText>(R.id.txtUser)
        val txtPass = findViewById<EditText>(R.id.txtPass)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val txtRegister = findViewById<TextView>(R.id.txtRegister)

        // User info views
        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)
        val tvUserRole = findViewById<TextView>(R.id.tvUserRole)
        val btnManageBooks = findViewById<Button>(R.id.btnManageBooks)
        val btnManageUsers = findViewById<Button>(R.id.btnManageUsers)
        val btnSubscription = findViewById<Button>(R.id.btnSubscription)
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        val btnSettings = findViewById<Button>(R.id.btnSettings)

        // KT trạng thái đăng nhập
        val savedUsername = sharedPreferences.getString("username", null)
        val savedRole = sharedPreferences.getString("role", null)

        if (savedUsername != null && savedRole != null) {
            showUserInfo(layoutLogin, layoutUserInfo, savedUsername, savedRole, tvWelcome, tvUserRole, btnManageBooks, btnManageUsers, btnSubscription)
        } else {
            showLoginForm(layoutLogin, layoutUserInfo)
        }

        btnBack.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        btnLogin.setOnClickListener {
            val username = txtUser.text.toString().trim()
            val password = txtPass.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Dùng FirebaseHelper để đăng nhập
            firebaseHelper.login(username, password) { role, message ->
                if (role != null) {
                    val editor = sharedPreferences.edit()
                    editor.putString("username", username)
                    editor.putString("role", role)
                    editor.apply()

                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                    
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                    finish()
                } else {
                    Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnManageBooks.setOnClickListener {
            startActivity(Intent(this, AdminActivity::class.java))
        }

        btnManageUsers.setOnClickListener {
            val intent = Intent(this, com.example.ngdungocsach.admin.ManageUsersActivity::class.java)
            startActivity(intent)
        }

        btnSubscription.setOnClickListener {
            val intent = Intent(this, com.example.ngdungocsach.user.SubscriptionActivity::class.java)
            startActivity(intent)
        }

        btnSettings.setOnClickListener {
            showFontSizeDialog()
        }

        btnLogout.setOnClickListener {
            val editor = sharedPreferences.edit()
            editor.clear()
            editor.apply()
            Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show()
            showLoginForm(layoutLogin, layoutUserInfo)
        }

        txtRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }
    }

    private fun showUserInfo(
        layoutLogin: View,
        layoutUserInfo: View,
        username: String,
        role: String,
        tvWelcome: TextView,
        tvUserRole: TextView,
        btnManageBooks: Button,
        btnManageUsers: Button,
        btnSubscription: Button
    ) {
        layoutLogin.visibility = View.GONE
        layoutUserInfo.visibility = View.VISIBLE
        tvWelcome.text = "Chào mừng bạn, $username!"
        tvUserRole.visibility = View.GONE
        
        if (role == "admin") {
            btnManageBooks.visibility = View.VISIBLE
            btnManageUsers.visibility = View.VISIBLE
            btnSubscription.visibility = View.GONE
        } else {
            btnManageBooks.visibility = View.GONE
            btnManageUsers.visibility = View.GONE
            btnSubscription.visibility = View.VISIBLE
        }
    }

    private fun showLoginForm(layoutLogin: View, layoutUserInfo: View) {
        layoutLogin.visibility = View.VISIBLE
        layoutUserInfo.visibility = View.GONE
    }

    private fun showFontSizeDialog() {
        val sizes = arrayOf("Nhỏ", "Trung bình", "Lớn")
        val currentSize = sharedPreferences.getInt("font_size", 1)

        MaterialAlertDialogBuilder(this)
            .setTitle("Chọn cỡ chữ")
            .setSingleChoiceItems(sizes, currentSize) { dialog, which ->
                val editor = sharedPreferences.edit()
                editor.putInt("font_size", which)
                editor.apply()
                recreate()
                dialog.dismiss()
            }
            .show()
    }
}
