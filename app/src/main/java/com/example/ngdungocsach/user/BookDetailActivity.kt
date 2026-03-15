package com.example.ngdungocsach.user

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.example.ngdungocsach.R
import com.example.ngdungocsach.database.DatabaseHelper
import com.example.ngdungocsach.ui.BaseActivity
import com.google.android.material.button.MaterialButton

class BookDetailActivity : BaseActivity() { // Đổi sang BaseActivity

    private lateinit var db: DatabaseHelper
    private var bookId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_detail)

        db = DatabaseHelper(this)

        val imgBook = findViewById<ImageView>(R.id.imgBook)
        val txtTitle = findViewById<TextView>(R.id.txtTitle)
        val txtAuthor = findViewById<TextView>(R.id.txtAuthor)
        val txtCategory = findViewById<TextView>(R.id.txtCategory)
        val txtDescription = findViewById<TextView>(R.id.txtDescription)
        val btnBack = findViewById<MaterialButton>(R.id.btnBack)
        val btnEditDescription = findViewById<MaterialButton>(R.id.btnEditDescription)
        val btnReadBook = findViewById<MaterialButton>(R.id.btnReadBook)
        val adminControls = findViewById<View>(R.id.adminControls)
        val btnEditBook = findViewById<MaterialButton>(R.id.btnEditBook)
        val btnDeleteBook = findViewById<MaterialButton>(R.id.btnDeleteBook)

        bookId = intent.getIntExtra("id", -1)
        val book = db.getBookById(bookId)

        if (book != null) {
            txtTitle.text = book.title
            txtAuthor.text = book.author
            txtCategory.text = "Thể loại: ${book.category}"
            txtDescription.text = if (book.description.isNotEmpty()) book.description else "Nội dung mô tả sách đang được cập nhật..."

            if (book.image.isNotEmpty()) {
                try {
                    imgBook.setImageURI(Uri.parse(book.image))
                } catch (e: Exception) {
                    imgBook.setImageResource(R.drawable.white)
                }
            } else {
                imgBook.setImageResource(R.drawable.white)
            }

            if (book.pdfUrl.isNotEmpty()) {
                btnReadBook.visibility = View.VISIBLE
                btnReadBook.setOnClickListener {
                    openPdf(book.pdfUrl)
                }
            } else {
                btnReadBook.visibility = View.GONE
            }
        }

        val sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val role = sharedPreferences.getString("role", null)

        if (role == "admin") {
            btnEditDescription.visibility = View.VISIBLE
            adminControls.visibility = View.VISIBLE
            
            btnEditDescription.setOnClickListener {
                showEditDescriptionDialog(txtDescription)
            }
            
            btnEditBook.setOnClickListener {
                val intent = Intent(this, com.example.ngdungocsach.admin.EditBookActivity::class.java)
                intent.putExtra("id", bookId)
                startActivity(intent)
            }
            
            btnDeleteBook.setOnClickListener {
                showDeleteConfirmationDialog()
            }
        }

        btnBack.setOnClickListener {
            finish()
        }
    }

    private fun openPdf(pdfUriString: String) {
        try {
            val uri = Uri.parse(pdfUriString)
            val intent = Intent(Intent.ACTION_VIEW)
            intent.setDataAndType(uri, "application/pdf")
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY)
            startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(this, "Không tìm thấy ứng dụng đọc PDF trên máy bạn", Toast.LENGTH_SHORT).show()
        }
    }

    private fun showEditDescriptionDialog(tvDescription: TextView) {
        val input = EditText(this)
        input.setText(db.getBookById(bookId)?.description)
        input.setTextColor(resources.getColor(R.color.black, theme)) // Đảm bảo màu chữ hiển thị rõ
        
        val container = LinearLayout(this)
        container.orientation = LinearLayout.VERTICAL
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        )
        lp.setMargins(50, 20, 50, 20)
        input.layoutParams = lp
        container.addView(input)

        com.google.android.material.dialog.MaterialAlertDialogBuilder(this)
            .setTitle("Chỉnh sửa mô tả")
            .setView(container)
            .setPositiveButton("Lưu") { _, _ ->
                val newDescription = input.text.toString()
                if (db.updateBookDescription(bookId, newDescription)) {
                    tvDescription.text = newDescription
                    Toast.makeText(this, "Đã cập nhật mô tả", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "Lỗi khi cập nhật", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Xóa sách")
            .setMessage("Bạn có chắc chắn muốn xóa cuốn sách này không?")
            .setPositiveButton("Xóa") { _, _ ->
                if (db.deleteBook(bookId)) {
                    Toast.makeText(this, "Đã xóa sách", Toast.LENGTH_SHORT).show()
                    finish()
                } else {
                    Toast.makeText(this, "Lỗi khi xóa sách", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
}
