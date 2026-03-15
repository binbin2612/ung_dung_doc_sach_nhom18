package com.example.ngdungocsach.user

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.EditText
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.ngdungocsach.R
import com.example.ngdungocsach.adapter.BookAdapter
import com.example.ngdungocsach.auth.LoginActivity
import com.example.ngdungocsach.database.DatabaseHelper
import com.example.ngdungocsach.model.Book
import com.example.ngdungocsach.ui.BaseActivity
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.textfield.TextInputEditText

class MainActivity : BaseActivity() { // Đổi sang BaseActivity

    private lateinit var rvBooks: RecyclerView
    private lateinit var db: DatabaseHelper
    private var fullBookList = ArrayList<Book>()
    private lateinit var searchView: TextInputEditText // Keep original name or use edtSearch
    private lateinit var edtSearch: TextInputEditText
    private lateinit var tvEmptyMessage: android.widget.TextView
    private lateinit var spinnerCategoryFilter: AutoCompleteTextView
    private val categories = arrayOf("Tất cả", "Ngôn tình", "Hành động", "Trinh thám", "Kinh dị", "Khoa học", "Kỹ năng sống", "Khác")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        db = DatabaseHelper(this)
        rvBooks = findViewById(R.id.rvBooks)
        edtSearch = findViewById(R.id.edtSearch)
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage)
        spinnerCategoryFilter = findViewById(R.id.spinnerCategoryFilter)

        val btnHome = findViewById<FloatingActionButton>(R.id.Home_FloatingActionButton)
        val btnLove = findViewById<FloatingActionButton>(R.id.Love_FloatingActionButton)
        val btnLogin = findViewById<FloatingActionButton>(R.id.Login_FloatingActionButton)

        rvBooks.layoutManager = LinearLayoutManager(this)

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        spinnerCategoryFilter.setAdapter(adapter)
        
        // Dùng post để thiết lập giá trị mặc định mà không kích hoạt filter sai lúc khởi tạo lại Activity
        spinnerCategoryFilter.post {
            spinnerCategoryFilter.setText("Tất cả", false)
        }

        spinnerCategoryFilter.setOnItemClickListener { _, _, _, _ ->
            applyFilters()
        }

        edtSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                applyFilters()
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        btnHome.setOnClickListener {
            edtSearch.setText("")
            spinnerCategoryFilter.setText("Tất cả", false)
            edtSearch.clearFocus()
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
        loadBooks()
    }

    private fun loadBooks() {
        fullBookList = db.getAllBooks()
        applyFilters()
    }

    private fun applyFilters() {
        val query = edtSearch.text.toString().lowercase().trim()
        val selectedCategory = spinnerCategoryFilter.text.toString().trim()

        val filteredList = ArrayList<Book>()
        for (book in fullBookList) {
            val matchesQuery = book.title.lowercase().contains(query) || book.author.lowercase().contains(query)
            
            // So sánh chính xác hoặc chấp nhận "Tất cả"
            val matchesCategory = selectedCategory == "Tất cả" || 
                                 selectedCategory.isEmpty() || 
                                 book.category.equals(selectedCategory, ignoreCase = true)

            if (matchesQuery && matchesCategory) {
                filteredList.add(book)
            }
        }
        rvBooks.adapter = BookAdapter(filteredList)

        if (filteredList.isEmpty()) {
            tvEmptyMessage.visibility = View.VISIBLE
        } else {
            tvEmptyMessage.visibility = View.GONE
        }
    }
}
