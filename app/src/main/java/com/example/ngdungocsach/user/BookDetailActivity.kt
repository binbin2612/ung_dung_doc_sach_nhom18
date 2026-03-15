package com.example.ngdungocsach.user

import android.app.AlertDialog
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ngdungocsach.R
import com.example.ngdungocsach.database.DatabaseHelper
import com.google.android.material.button.MaterialButton

class BookDetailActivity : AppCompatActivity() {

    private lateinit var db: DatabaseHelper
    private var bookId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_detail)

        db = DatabaseHelper(this)

        val imgBook = findViewById<ImageView>(R.id.imgBook)
        val txtTitle = findViewById<TextView>(R.id.txtTitle)
        val txtAuthor = findViewById<TextView>(R.id.txtAuthor)
        val txtDescription = findViewById<TextView>(R.id.txtDescription)
        val btnBack = findViewById<MaterialButton>(R.id.btnBack)
        val btnEditDescription = findViewById<MaterialButton>(R.id.btnEditDescription)

        bookId = intent.getIntExtra("id", -1)
        val book = db.getBookById(bookId)

        if (book != null) {
            txtTitle.text = book.title
            txtAuthor.text = book.author
            txtDescription.text = if (book.description.isNotEmpty()) book.description else "Nội dung mô tả sách đang được cập nhật..."

            if (book.image.isNotEmpty()) {
                try {
                    imgBook.setImageURI(Uri.parse(book.image))
                } catch (e: Exception) {
                    imgBook.setImageResource(R.drawable.book_sample)
                }
            } else {
                imgBook.setImageResource(R.drawable.book_sample)
            }
        }

        // Kiểm tra quyền admin
        val sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val role = sharedPreferences.getString("role", null)

        if (role == "admin") {
            btnEditDescription.visibility = View.VISIBLE
            btnEditDescription.setOnClickListener {
                showEditDescriptionDialog(txtDescription)
            }
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun showEditDescriptionDialog(tvDescription: TextView) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("Chỉnh sửa mô tả")

        val input = EditText(this)
        input.setText(db.getBookById(bookId)?.description)
        input.setPadding(50, 20, 50, 20)
        builder.setView(input)

        builder.setPositiveButton("Lưu") { _, _ ->
            val newDescription = input.text.toString()
            if (db.updateBookDescription(bookId, newDescription)) {
                tvDescription.text = newDescription
                Toast.makeText(this, "Đã cập nhật mô tả", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Lỗi khi cập nhật", Toast.LENGTH_SHORT).show()
            }
        }
        builder.setNegativeButton("Hủy", null)
        builder.show()
    }
}