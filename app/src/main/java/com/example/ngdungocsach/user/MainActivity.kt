package com.example.ngdungocsach.user

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ngdungocsach.R
import com.example.ngdungocsach.adapter.BookAdapter
import com.example.ngdungocsach.auth.LoginActivity
import com.example.ngdungocsach.database.DatabaseHelper
import com.example.ngdungocsach.model.Book
import com.example.ngdungocsach.ui.BaseActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton

class MainActivity : BaseActivity() { // Đổi sang BaseActivity

    private lateinit var rvBooks: RecyclerView
    private lateinit var db: DatabaseHelper
    private var fullBookList = ArrayList<Book>()
    private lateinit var searchView: SearchView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = DatabaseHelper(this)
        rvBooks = findViewById(R.id.rvBooks)
        searchView = findViewById(R.id.searchView)

        val btnHome = findViewById<FloatingActionButton>(R.id.Home_FloatingActionButton)
        val btnLove = findViewById<FloatingActionButton>(R.id.Love_FloatingActionButton)
        val btnLogin = findViewById<FloatingActionButton>(R.id.Login_FloatingActionButton)

        rvBooks.layoutManager = LinearLayoutManager(this)

        searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                filterBooks(query)
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                filterBooks(newText)
                return true
            }
        })

        btnHome.setOnClickListener {
            searchView.setQuery("", false)
            searchView.clearFocus()
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
            startActivity(Intent(this, LoginActivity::class.java))
        }

        loadBooks()
    }

    override fun onResume() {
        super.onResume()
        if (searchView.query.isEmpty()) {
            loadBooks()
        }
    }

    private fun loadBooks() {
        fullBookList = db.getAllBooks()
        rvBooks.adapter = BookAdapter(fullBookList)
    }

    private fun filterBooks(query: String?) {
        val filteredList = ArrayList<Book>()
        if (query.isNullOrEmpty()) {
            filteredList.addAll(fullBookList)
        } else {
            val lowerCaseQuery = query.lowercase().trim()
            for (book in fullBookList) {
                if (book.title.lowercase().contains(lowerCaseQuery) ||
                    book.author.lowercase().contains(lowerCaseQuery)
                ) {
                    filteredList.add(book)
                }
            }
        }
        rvBooks.adapter = BookAdapter(filteredList)
    }
}
