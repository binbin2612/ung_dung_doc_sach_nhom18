package com.example.ngdungocsach.admin

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import com.example.ngdungocsach.database.FirebaseHelper
import com.example.ngdungocsach.database.SupabaseHelper
import com.example.ngdungocsach.model.Book
import com.example.ngdungocsach.R
import com.example.ngdungocsach.user.MainActivity
import com.example.ngdungocsach.ui.BaseActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.MaterialAutoCompleteTextView

class AdminActivity : BaseActivity() { // Đổi sang BaseActivity

    private lateinit var firebaseHelper: FirebaseHelper
    private lateinit var supabaseHelper: SupabaseHelper
    private var selectedImageUri: Uri? = null
    private var selectedPdfUri: Uri? = null
    private lateinit var imgBook: ImageView
    private lateinit var tvPdfStatus: TextView
    private lateinit var spinnerCategory: MaterialAutoCompleteTextView
    private val categories = arrayOf("Ngôn tình", "Hành động", "Trinh thám", "Kinh dị", "Khoa học", "Kỹ năng sống", "Khác")

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        firebaseHelper = FirebaseHelper()
        supabaseHelper = SupabaseHelper(this)

        val btnBack = findViewById<MaterialButton>(R.id.btnBack)
        val txtTitle = findViewById<TextInputEditText>(R.id.txtTitle)
        val txtAuthor = findViewById<TextInputEditText>(R.id.txtAuthor)
        val txtDescription = findViewById<TextInputEditText>(R.id.txtDescription)
        val btnChooseImage = findViewById<MaterialButton>(R.id.btnChooseImage)
        val btnChoosePdf = findViewById<MaterialButton>(R.id.btnChoosePdf)
        imgBook = findViewById(R.id.imgBook)
        tvPdfStatus = findViewById(R.id.tvPdfStatus)
        spinnerCategory = findViewById<MaterialAutoCompleteTextView>(R.id.spinnerCategory)
        val btnAdd = findViewById<MaterialButton>(R.id.btnAdd)

        val adapter = ArrayAdapter(this, android.R.layout.simple_dropdown_item_1line, categories)
        spinnerCategory.setAdapter(adapter)

        btnBack.setOnClickListener {
            val intent = Intent(this, com.example.ngdungocsach.user.MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            startActivity(intent)
            finish()
        }

        val pickImage = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                try {
                    contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    selectedImageUri = it
                    imgBook.setImageURI(it)
                } catch (e: Exception) {
                    Toast.makeText(this, "Lỗi khi chọn ảnh", Toast.LENGTH_SHORT).show()
                }
            }
        }

        val pickPdf = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                try {
                    contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                    selectedPdfUri = it
                    tvPdfStatus.text = "Đã chọn: ${getFileName(it)}"
                } catch (e: Exception) {
                    Toast.makeText(this, "Lỗi khi chọn PDF", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnChooseImage.setOnClickListener { pickImage.launch(arrayOf("image/*")) }
        btnChoosePdf.setOnClickListener { pickPdf.launch(arrayOf("application/pdf")) }

        btnAdd.setOnClickListener {
            val title = txtTitle.text.toString()
            val author = txtAuthor.text.toString()
            val description = txtDescription.text.toString()
            val category = spinnerCategory.text.toString()

            if (title.isEmpty() || author.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val progressDialog = android.app.ProgressDialog(this)
            progressDialog.setMessage("Đang lưu sách...")
            progressDialog.setCancelable(false)
            progressDialog.show()

            var uploadedImageUrl = ""
            var uploadedPdfUrl = ""
            var uploadCount = 0
            val totalUploads = (if (selectedImageUri != null) 1 else 0) + (if (selectedPdfUri != null) 1 else 0)

            fun saveBookToFirestore() {
                val newBook = Book(
                    id = "",
                    title = title,
                    author = author,
                    image = uploadedImageUrl,
                    description = description,
                    pdfUrl = uploadedPdfUrl,
                    category = category
                )
                firebaseHelper.addBook(newBook) { success ->
                    progressDialog.dismiss()
                    if (success) {
                        Toast.makeText(this, "Thêm sách thành công", Toast.LENGTH_SHORT).show()
                        txtTitle.text?.clear()
                        txtAuthor.text?.clear()
                        txtDescription.text?.clear()
                        imgBook.setImageResource(R.drawable.white)
                        tvPdfStatus.text = "Chưa chọn file PDF"
                        selectedImageUri = null
                        selectedPdfUri = null
                        spinnerCategory.setText("", false)
                    } else {
                        Toast.makeText(this, "Thêm sách thất bại", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            if (totalUploads == 0) {
                saveBookToFirestore()
            } else {
                var isFailed = false
                selectedImageUri?.let { uri ->
                    supabaseHelper.uploadFile(uri, "book_images") { url ->
                        if (isFailed) return@uploadFile
                        uploadCount++
                        if (url != null) {
                            uploadedImageUrl = url
                            if (uploadCount == totalUploads) saveBookToFirestore()
                        } else {
                            isFailed = true
                            progressDialog.dismiss()
                            Toast.makeText(this, "Lỗi upload Ảnh lên Supabase", Toast.LENGTH_LONG).show()
                        }
                    }
                }
                selectedPdfUri?.let { uri ->
                    supabaseHelper.uploadFile(uri, "book_pdfs") { url ->
                        if (isFailed) return@uploadFile
                        uploadCount++
                        if (url != null) {
                            uploadedPdfUrl = url
                            if (uploadCount == totalUploads) saveBookToFirestore()
                        } else {
                            isFailed = true
                            progressDialog.dismiss()
                            Toast.makeText(this, "Lỗi upload PDF lên Supabase", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        }
    }

    private fun getFileName(uri: Uri): String {
        return uri.path?.substringAfterLast('/') ?: "file_pdf"
    }
}
