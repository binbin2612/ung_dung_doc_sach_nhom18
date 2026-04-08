package com.example.ngdungocsach.admin

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ngdungocsach.R
import com.example.ngdungocsach.database.DatabaseHelper
import com.example.ngdungocsach.ui.BaseActivity
import com.google.android.material.button.MaterialButton

class ManageUsersActivity : BaseActivity() {

    private lateinit var db: DatabaseHelper
    private lateinit var rvUsers: RecyclerView
    private lateinit var userList: ArrayList<Triple<Int, String, String>>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_users)

        db = DatabaseHelper(this)
        rvUsers = findViewById(R.id.rvUsers)
        val btnBack = findViewById<MaterialButton>(R.id.btnBack)

        rvUsers.layoutManager = LinearLayoutManager(this)
        loadUsers()

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun loadUsers() {
        userList = db.getAllAccounts()
        rvUsers.adapter = UserAdapter(userList) { userId, username ->
            showDeleteDialog(userId, username)
        }
    }

    private fun showDeleteDialog(userId: Int, username: String) {
        if (username == "admin") {
            Toast.makeText(this, "Không thể xóa tài khoản Admin gốc", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Xóa người dùng")
            .setMessage("Bạn có chắc chắn muốn xóa tài khoản '$username' không?")
            .setPositiveButton("Xóa") { _, _ ->
                if (db.deleteAccount(userId)) {
                    Toast.makeText(this, "Đã xóa người dùng", Toast.LENGTH_SHORT).show()
                    loadUsers()
                } else {
                    Toast.makeText(this, "Lỗi khi xóa", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
}
