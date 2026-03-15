package com.example.ngdungocsach.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatDelegate
import com.example.ngdungocsach.database.DatabaseHelper
import com.example.ngdungocsach.admin.AdminActivity
import com.example.ngdungocsach.user.MainActivity
import com.example.ngdungocsach.R
import com.example.ngdungocsach.ui.BaseActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.dialog.MaterialAlertDialogBuilder

class LoginActivity : BaseActivity() { // Đổi sang BaseActivity

    private lateinit var db: DatabaseHelper
    private lateinit var sharedPreferences: android.content.SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        db = DatabaseHelper(this)
        sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)

        val btnBack = findViewById<MaterialButton>(R.id.btnBack)
        val layoutLogin = findViewById<LinearLayout>(R.id.layoutLogin)
        val layoutUserInfo = findViewById<LinearLayout>(R.id.layoutUserInfo)

        // Login views
        val txtUser = findViewById<EditText>(R.id.txtUser)
        val txtPass = findViewById<EditText>(R.id.txtPass)
        val btnLogin = findViewById<Button>(R.id.btnLogin)
        val txtRegister = findViewById<TextView>(R.id.txtRegister)

        // User info views
        val tvWelcome = findViewById<TextView>(R.id.tvWelcome)
        val tvUserRole = findViewById<TextView>(R.id.tvUserRole)
        val btnManageBooks = findViewById<Button>(R.id.btnManageBooks)
        val btnLogout = findViewById<Button>(R.id.btnLogout)
        val btnSettings = findViewById<Button>(R.id.btnSettings)

        // Kiểm tra trạng thái đăng nhập
        val savedUsername = sharedPreferences.getString("username", null)
        val savedRole = sharedPreferences.getString("role", null)

        if (savedUsername != null && savedRole != null) {
            showUserInfo(layoutLogin, layoutUserInfo, savedUsername, savedRole, tvWelcome, tvUserRole, btnManageBooks)
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
            val username = txtUser.text.toString()
            val password = txtPass.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val role = db.checkLogin(username, password)

            if (role != null) {
                val editor = sharedPreferences.edit()
                editor.putString("username", username)
                editor.putString("role", role)
                editor.apply()

                Toast.makeText(this, "Đăng nhập thành công", Toast.LENGTH_SHORT).show()
                showUserInfo(layoutLogin, layoutUserInfo, username, role, tvWelcome, tvUserRole, btnManageBooks)
            } else {
                Toast.makeText(this, "Sai tài khoản hoặc mật khẩu", Toast.LENGTH_SHORT).show()
            }
        }

        btnManageBooks.setOnClickListener {
            startActivity(Intent(this, AdminActivity::class.java))
        }

        btnSettings.setOnClickListener {
            showSettingsDialog()
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
        btnManageBooks: Button
    ) {
        layoutLogin.visibility = View.GONE
        layoutUserInfo.visibility = View.VISIBLE
        tvWelcome.text = "Chào mừng, $username!"
        tvUserRole.text = "Vai trò: ${role.replaceFirstChar { it.uppercase() }}"
        btnManageBooks.visibility = if (role == "admin") View.VISIBLE else View.GONE
    }

    private fun showLoginForm(layoutLogin: View, layoutUserInfo: View) {
        layoutLogin.visibility = View.VISIBLE
        layoutUserInfo.visibility = View.GONE
    }

    private fun showSettingsDialog() {
        val options = arrayOf("Chủ đề (Sáng/Tối)", "Cỡ chữ")
        MaterialAlertDialogBuilder(this)
            .setTitle("Cài đặt hệ thống")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showThemeDialog()
                    1 -> showFontSizeDialog()
                }
            }
            .show()
    }

    private fun showThemeDialog() {
        val themes = arrayOf("Sáng", "Tối", "Theo hệ thống")
        val currentTheme = sharedPreferences.getInt("theme_mode", 2)

        MaterialAlertDialogBuilder(this)
            .setTitle("Chọn chủ đề")
            .setSingleChoiceItems(themes, currentTheme) { dialog, which ->
                val editor = sharedPreferences.edit()
                editor.putInt("theme_mode", which)
                editor.apply()

                when (which) {
                    0 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
                    1 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
                    2 -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
                }
                dialog.dismiss()
            }
            .show()
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
                
                Toast.makeText(this, "Cài đặt sẽ được áp dụng ngay bây giờ", Toast.LENGTH_SHORT).show()
                recreate() // Khởi động lại activity để áp dụng font
                dialog.dismiss()
            }
            .show()
    }
}
