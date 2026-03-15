package com.example.ngdungocsach.auth

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.ngdungocsach.database.DatabaseHelper
import com.example.ngdungocsach.admin.AdminActivity
import com.example.ngdungocsach.user.MainActivity
import com.example.ngdungocsach.R
import com.google.android.material.button.MaterialButton

class LoginActivity : AppCompatActivity() {

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


        // Kiểm tra trạng thái đăng nhập
        val savedUsername = sharedPreferences.getString("username", null)
        val savedRole = sharedPreferences.getString("role", null)

        if (savedUsername != null && savedRole != null) {
            showUserInfo(layoutLogin, layoutUserInfo, savedUsername, savedRole, tvWelcome, tvUserRole, btnManageBooks)
        } else {
            showLoginForm(layoutLogin, layoutUserInfo)
        }

        // Nút Home (quay lại)
        val goBackHome = {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        btnBack.setOnClickListener { goBackHome() }
       

        btnLogin.setOnClickListener {
            val username = txtUser.text.toString()
            val password = txtPass.text.toString()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val role = db.checkLogin(username, password)

            if (role != null) {
                // Lưu trạng thái đăng nhập
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

        btnLogout.setOnClickListener {
            // Xóa trạng thái đăng nhập
            val editor = sharedPreferences.edit()
            editor.clear()
            editor.apply()

            Toast.makeText(this, "Đã đăng xuất", Toast.LENGTH_SHORT).show()
            showLoginForm(layoutLogin, layoutUserInfo)
        }

        txtRegister.setOnClickListener {
            // Chuyển sang màn hình đăng ký
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
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

        // Chỉ hiển thị nút Quản lý sách nếu là admin
        if (role == "admin") {
            btnManageBooks.visibility = View.VISIBLE
        } else {
            btnManageBooks.visibility = View.GONE
        }
    }

    private fun showLoginForm(layoutLogin: View, layoutUserInfo: View) {
        layoutLogin.visibility = View.VISIBLE
        layoutUserInfo.visibility = View.GONE
    }
}