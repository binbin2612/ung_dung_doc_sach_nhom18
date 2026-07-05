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
import com.example.ngdungocsach.database.FirebaseHelper
import com.example.ngdungocsach.model.Book
import com.example.ngdungocsach.ui.BaseActivity
import com.google.android.material.button.MaterialButton
import com.bumptech.glide.Glide

class BookDetailActivity : BaseActivity() { // Đổi sang BaseActivity

    private lateinit var firebaseHelper: FirebaseHelper
    private var bookId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_book_detail)

        firebaseHelper = FirebaseHelper()

        val imgBook = findViewById<ImageView>(R.id.imgBook)
        val txtTitle = findViewById<TextView>(R.id.txtTitle)
        val txtAuthor = findViewById<TextView>(R.id.txtAuthor)
        val txtCategory = findViewById<TextView>(R.id.txtCategory)
        val txtDescription = findViewById<TextView>(R.id.txtDescription)
        val btnBack = findViewById<MaterialButton>(R.id.btnBack)
        val btnFavorite = findViewById<MaterialButton>(R.id.btnFavorite)
        val btnEditDescription = findViewById<MaterialButton>(R.id.btnEditDescription)
        val btnReadBook = findViewById<MaterialButton>(R.id.btnReadBook)
        val adminControls = findViewById<LinearLayout>(R.id.adminControls)
        val btnEditBook = findViewById<MaterialButton>(R.id.btnEditBook)
        val btnDeleteBook = findViewById<MaterialButton>(R.id.btnDeleteBook)

        bookId = intent.getStringExtra("id") ?: ""
        
        val sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val role = sharedPreferences.getString("role", null)
        val username = sharedPreferences.getString("username", null)

        if (bookId.isNotEmpty()) {
            // Kiểm tra trạng thái yêu thích
            if (username != null) {
                firebaseHelper.isFavorite(username, bookId) { isFav ->
                    updateFavoriteUI(btnFavorite, isFav)
                }
            }

            btnFavorite.setOnClickListener {
                if (username == null) {
                    Toast.makeText(this, "Vui lòng đăng nhập để thêm vào yêu thích", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }
                
                firebaseHelper.toggleFavorite(username, bookId) { success ->
                    if (success) {
                        firebaseHelper.isFavorite(username, bookId) { isFav ->
                            updateFavoriteUI(btnFavorite, isFav)
                            val msg = if (isFav) "Đã thêm vào yêu thích" else "Đã xóa khỏi yêu thích"
                            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            }

            firebaseHelper.getBookById(bookId) { book ->
                if (book != null) {
                    // Nếu sách bị ẩn và không phải admin, không cho xem chi tiết
                    if (book.isHidden && role != "admin") {
                        Toast.makeText(this, "Sách này hiện không khả dụng", Toast.LENGTH_SHORT).show()
                        finish()
                        return@getBookById
                    }

                    txtTitle.text = book.title
                    txtAuthor.text = book.author
                    txtCategory.text = "Thể loại: ${book.category}"
                    txtDescription.text = if (book.description.isNotEmpty()) book.description else "Nội dung mô tả sách đang được cập nhật..."

                    if (book.image.isNotEmpty() && book.image.startsWith("http")) {
                        Glide.with(this@BookDetailActivity)
                            .load(book.image)
                            .placeholder(R.drawable.white)
                            .error(R.drawable.white)
                            .into(imgBook)
                    } else {
                        imgBook.setImageResource(R.drawable.white)
                    }

                    if (book.pdfUrl.isNotEmpty() && book.pdfUrl.startsWith("http")) {
                        btnReadBook.visibility = View.VISIBLE
                        btnReadBook.setOnClickListener {
                            if (role == "admin") {
                                openPdf(book.pdfUrl)
                            } else if (username != null) {
                                firebaseHelper.getSubscriptionExpiry(username) { expiry ->
                                    if (expiry > System.currentTimeMillis()) {
                                        openPdf(book.pdfUrl)
                                    } else {
                                        showSubscriptionRequiredDialog()
                                    }
                                }
                            } else {
                                showSubscriptionRequiredDialog()
                            }
                        }
                    } else {
                        btnReadBook.visibility = View.GONE
                    }
                }
            }
        }

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
            val intent = Intent(this, MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun updateFavoriteUI(btn: MaterialButton, isFav: Boolean) {
        if (isFav) {
            btn.setIconResource(R.drawable.ic_favorite)
            btn.setIconTintResource(R.color.error) // Màu đỏ
        } else {
            btn.setIconResource(R.drawable.ic_heart_outline)
            btn.setIconTintResource(R.color.gray)
        }
    }

    private fun openPdf(pdfUrl: String) {
        val intent = Intent(this, ReadBookActivity::class.java)
        intent.putExtra("pdfUrl", pdfUrl)
        startActivity(intent)
    }

    private fun showSubscriptionRequiredDialog() {
        AlertDialog.Builder(this)
            .setTitle("Yêu cầu đăng ký")
            .setMessage("Bạn cần đăng ký gói premium để đọc cuốn sách này. Bạn có muốn chuyển đến trang đăng ký không?")
            .setPositiveButton("Đăng ký") { _, _ ->
                val intent = Intent(this, SubscriptionActivity::class.java)
                startActivity(intent)
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun showEditDescriptionDialog(tvDescription: TextView) {
        val input = EditText(this)
        
        firebaseHelper.getBookById(bookId) { book ->
            input.setText(book?.description)
            input.setTextColor(resources.getColor(R.color.black, theme))
            
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
                    book?.let {
                        val updatedBook = it.copy(description = newDescription)
                        firebaseHelper.updateBook(updatedBook) { success ->
                            if (success) {
                                tvDescription.text = newDescription
                                Toast.makeText(this, "Đã cập nhật mô tả", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(this, "Lỗi khi cập nhật", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
                .setNegativeButton("Hủy", null)
                .show()
        }
    }

    private fun showDeleteConfirmationDialog() {
        AlertDialog.Builder(this)
            .setTitle("Xóa sách")
            .setMessage("Bạn có chắc chắn muốn xóa cuốn sách này không?")
            .setPositiveButton("Xóa") { _, _ ->
                firebaseHelper.deleteBook(bookId) { success ->
                    if (success) {
                        Toast.makeText(this, "Đã xóa sách", Toast.LENGTH_SHORT).show()
                        finish()
                    } else {
                        Toast.makeText(this, "Lỗi khi xóa sách", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
}
