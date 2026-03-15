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
import com.example.ngdungocsach.user.MainActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText

class AdminActivity : AppCompatActivity() {

    lateinit var db: DatabaseHelper
    private var selectedImageUri: Uri? = null
    lateinit var imgBook: ImageView

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        db = DatabaseHelper(this)

        val btnBack = findViewById<MaterialButton>(R.id.btnBack)
        val txtTitle = findViewById<TextInputEditText>(R.id.txtTitle)
        val txtAuthor = findViewById<TextInputEditText>(R.id.txtAuthor)
        val txtDescription = findViewById<TextInputEditText>(R.id.txtDescription)
        val btnChooseImage = findViewById<MaterialButton>(R.id.btnChooseImage)
        imgBook = findViewById(R.id.imgBook)
        val btnAdd = findViewById<MaterialButton>(R.id.btnAdd)

        // Nút quay lại trang chủ
        btnBack.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        // Đăng ký bộ chọn hình ảnh
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

        btnAdd.setOnClickListener {
            val title = txtTitle.text.toString()
            val author = txtAuthor.text.toString()
            val description = txtDescription.text.toString()
            val imagePath = selectedImageUri?.toString() ?: ""

            if (title.isNotEmpty() && author.isNotEmpty()) {
                db.addBook(title, author, imagePath, description)
                Toast.makeText(this, "Thêm sách thành công", Toast.LENGTH_SHORT).show()
                
                // Reset form
                txtTitle.text?.clear()
                txtAuthor.text?.clear()
                txtDescription.text?.clear()
                imgBook.setImageResource(R.drawable.book_sample)
                selectedImageUri = null
            } else {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
