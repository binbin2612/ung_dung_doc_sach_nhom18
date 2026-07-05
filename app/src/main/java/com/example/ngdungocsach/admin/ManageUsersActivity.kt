package com.example.ngdungocsach.admin

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ngdungocsach.R
import com.example.ngdungocsach.database.FirebaseHelper
import com.example.ngdungocsach.model.User
import com.example.ngdungocsach.ui.BaseActivity
import com.example.ngdungocsach.ui.UserAdapter
import com.google.android.material.button.MaterialButton

class ManageUsersActivity : BaseActivity() {

    private lateinit var firebaseHelper: FirebaseHelper
    private lateinit var rvUsers: RecyclerView
    private lateinit var progressBar: ProgressBar
    private lateinit var userList: List<User>
    private var showHidden = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_users)

        firebaseHelper = FirebaseHelper()
        rvUsers = findViewById(R.id.rvUsers)
        progressBar = findViewById(R.id.progressBar)
        val btnBack = findViewById<MaterialButton>(R.id.btnBack)
        val btnToggleView = findViewById<MaterialButton>(R.id.btnToggleView)

        rvUsers.layoutManager = LinearLayoutManager(this)
        loadUsers()

        btnToggleView.setOnClickListener {
            showHidden = !showHidden
            btnToggleView.text = if (showHidden) "Hiện người dùng hoạt động" else "Xem danh sách ẩn"
            loadUsers()
        }

        btnBack.setOnClickListener {
            val intent = Intent(this, com.example.ngdungocsach.user.MainActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }
    }

    private fun loadUsers() {
        progressBar.visibility = View.VISIBLE
        firebaseHelper.getAllAccounts { list ->
            progressBar.visibility = View.GONE
            // Lọc danh sách dựa trên trạng thái showHidden
            userList = if (showHidden) {
                list.filter { it.isHidden }
            } else {
                list.filter { !it.isHidden || it.username == "admin" }
            }

            rvUsers.adapter = UserAdapter(
                userList,
                onDeleteClick = { username -> showDeleteDialog(username) },
                onBlockClick = { user ->
                    val newState = !user.isBlocked
                    firebaseHelper.toggleBlockUser(user.username, newState) { success ->
                        if (success) {
                            Toast.makeText(this, if (newState) "Đã chặn ${user.username}" else "Đã mở chặn", Toast.LENGTH_SHORT).show()
                            loadUsers()
                        }
                    }
                },
                onHideClick = { user ->
                    val newState = !user.isHidden
                    firebaseHelper.toggleHideUser(user.username, newState) { success ->
                        if (success) {
                            Toast.makeText(this, if (newState) "Đã ẩn ${user.username}" else "Đã hiện lại", Toast.LENGTH_SHORT).show()
                            loadUsers()
                        }
                    }
                }
            )
        }
    }

    private fun showDeleteDialog(username: String) {
        if (username == "admin") {
            Toast.makeText(this, "Không thể xóa tài khoản Admin gốc", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Xóa người dùng")
            .setMessage("Bạn có chắc chắn muốn xóa tài khoản '$username' không?")
            .setPositiveButton("Xóa") { _, _ ->
                firebaseHelper.deleteAccount(username) { success ->
                    if (success) {
                        Toast.makeText(this, "Đã xóa người dùng", Toast.LENGTH_SHORT).show()
                        loadUsers()
                    } else {
                        Toast.makeText(this, "Lỗi khi xóa", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
}
