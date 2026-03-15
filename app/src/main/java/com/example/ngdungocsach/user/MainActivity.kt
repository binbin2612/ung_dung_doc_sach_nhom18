package com.example.ngdungocsach.user

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ngdungocsach.R
import com.example.ngdungocsach.adapter.BookAdapter
import com.example.ngdungocsach.auth.LoginActivity
import com.example.ngdungocsach.database.DatabaseHelper
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : AppCompatActivity() {

    private lateinit var rvBooks: RecyclerView
    private lateinit var db: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val btnHome = findViewById<FloatingActionButton>(R.id.Home_FloatingActionButton)
        val btnLove = findViewById<FloatingActionButton>(R.id.Love_FloatingActionButton)
        val btnLogin = findViewById<FloatingActionButton>(R.id.Login_FloatingActionButton)

        btnHome.setOnClickListener { 
            // Refresh danh sách
            loadBooks()
            Toast.makeText(this, "Đã làm mới danh sách", Toast.LENGTH_SHORT).show()
        }

        btnLove.setOnClickListener {
            val sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            if (sharedPreferences.contains("username")) {
                val intent = Intent(this, FavoriteActivity::class.java)
                startActivity(intent)
            } else {
                Toast.makeText(this, "Vui lòng đăng nhập để xem yêu thích", Toast.LENGTH_SHORT).show()
                startActivity(Intent(this, LoginActivity::class.java))
            }
        }

        btnLogin.setOnClickListener {
            val sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
            if (sharedPreferences.contains("username")) {
                // Nếu đã đăng nhập, vào trang Profile
                val intent = Intent(this, LoginActivity::class.java)
                startActivity(intent)
            } else {
                // Nếu chưa, vào trang Login
                startActivity(Intent(this, LoginActivity::class.java))
            }
        }

        rvBooks = findViewById(R.id.rvBooks)
        db = DatabaseHelper(this)

        rvBooks.layoutManager = LinearLayoutManager(this)
        loadBooks()
    }

    override fun onResume() {
        super.onResume()
        loadBooks()
    }

    private fun loadBooks() {
        val bookList = db.getAllBooks()
        rvBooks.adapter = BookAdapter(bookList)
    }
}