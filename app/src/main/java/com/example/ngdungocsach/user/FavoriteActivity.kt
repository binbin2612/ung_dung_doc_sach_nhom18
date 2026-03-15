package com.example.ngdungocsach.user

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ngdungocsach.R
import com.example.ngdungocsach.adapter.BookAdapter
import com.example.ngdungocsach.database.DatabaseHelper
import com.google.android.material.button.MaterialButton

class FavoriteActivity : AppCompatActivity() {

    private lateinit var rvFavorite: RecyclerView
    private lateinit var tvNoFavorite: TextView
    private lateinit var db: DatabaseHelper

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_favorite)

        val btnBack = findViewById<MaterialButton>(R.id.btnBack)
        rvFavorite = findViewById(R.id.rvFavorite)
        tvNoFavorite = findViewById(R.id.tvNoFavorite)

        db = DatabaseHelper(this)

        rvFavorite.layoutManager = LinearLayoutManager(this)
        loadFavorites()

        btnBack.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }
    }

    override fun onResume() {
        super.onResume()
        loadFavorites()
    }

    private fun loadFavorites() {
        val sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val username = sharedPreferences.getString("username", null)

        if (username != null) {
            val favoriteList = db.getFavoriteBooks(username)
            
            if (favoriteList.isEmpty()) {
                rvFavorite.visibility = View.GONE
                tvNoFavorite.visibility = View.VISIBLE
                tvNoFavorite.text = "Chưa có sách yêu thích"
            } else {
                rvFavorite.visibility = View.VISIBLE
                tvNoFavorite.visibility = View.GONE
                rvFavorite.adapter = BookAdapter(favoriteList)
            }
        } else {
            rvFavorite.visibility = View.GONE
            tvNoFavorite.visibility = View.VISIBLE
            tvNoFavorite.text = "Vui lòng đăng nhập để xem sách yêu thích"
        }
    }
}
