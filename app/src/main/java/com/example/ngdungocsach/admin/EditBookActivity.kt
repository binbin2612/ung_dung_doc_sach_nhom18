package com.example.ngdungocsach.admin

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.ngdungocsach.database.DatabaseHelper
import com.example.ngdungocsach.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class EditBookActivity : AppCompatActivity() {

    private lateinit var db: DatabaseHelper
    private var selectedImageUri: Uri? = null
    private lateinit var imgBook: ImageView
    private var bookId: Int = -1

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_book)

        db = DatabaseHelper(this)

        val btnBack = findViewById<MaterialButton>(R.id.btnBack)
        val txtTitle = findViewById<TextInputEditText>(R.id.txtTitle)
        val txtAuthor = findViewById<TextInputEditText>(R.id.txtAuthor)
        val txtDescription = findViewById<TextInputEditText>(R.id.txtDescription)
        val btnChooseImage = findViewById<MaterialButton>(R.id.btnChooseImage)
        imgBook = findViewById(R.id.imgBook)
        val btnUpdate = findViewById<MaterialButton>(R.id.btnUpdate)

        // Nhận ID từ Intent và lấy dữ liệu mới nhất từ DB
        bookId = intent.getIntExtra("id", -1)
        val book = db.getBookById(bookId)

        if (book != null) {
            txtTitle.setText(book.title)
            txtAuthor.setText(book.author)
            txtDescription.setText(book.description)
            if (book.image.isNotEmpty()) {
                selectedImageUri = Uri.parse(book.image)
                try {
                    imgBook.setImageURI(selectedImageUri)
                } catch (e: Exception) {
                    imgBook.setImageResource(R.drawable.book_sample)
                }
            }
        }

        btnBack.setOnClickListener {
            finish()
        }

        val pickImage = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                try {
                    contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    selectedImageUri = it
                    imgBook.setImageURI(it)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Không thể lấy quyền truy cập ảnh", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnChooseImage.setOnClickListener {
            pickImage.launch(arrayOf("image/*"))
        }

        btnUpdate.setOnClickListener {
            val updatedTitle = txtTitle.text.toString()
            val updatedAuthor = txtAuthor.text.toString()
            val updatedDescription = txtDescription.text.toString()
            val imagePath = selectedImageUri?.toString() ?: ""

            if (updatedTitle.isNotEmpty() && updatedAuthor.isNotEmpty()) {
                if (db.updateBook(bookId, updatedTitle, updatedAuthor, imagePath, updatedDescription)) {
                    Toast.makeText(this, "Cập nhật thành công", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Cập nhật thất bại", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
            }
        }
    }
}