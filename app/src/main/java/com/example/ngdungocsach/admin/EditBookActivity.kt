package com.example.ngdungocsach.admin

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import android.widget.ArrayAdapter
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.example.ngdungocsach.database.FirebaseHelper
import com.example.ngdungocsach.database.SupabaseHelper
import com.example.ngdungocsach.model.Book
import com.example.ngdungocsach.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.MaterialAutoCompleteTextView

class EditBookActivity : AppCompatActivity() {

    private lateinit var firebaseHelper: FirebaseHelper
    private lateinit var supabaseHelper: SupabaseHelper
    private var selectedImageUri: Uri? = null
    private var selectedPdfUri: Uri? = null
    private lateinit var imgBook: ImageView
    private lateinit var txtPdfName: TextView
    private var bookId: String = ""
    private var currentHiddenStatus: Boolean = false
    private lateinit var spinnerCategory: MaterialAutoCompleteTextView
    private val categories = arrayOf("Ngôn tình", "Hành động", "Trinh thám", "Kinh dị", "Khoa học", "Kỹ năng sống", "Khác")

    @SuppressLint("MissingInflatedId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_book)

        firebaseHelper = FirebaseHelper()
        supabaseHelper = SupabaseHelper(this)

        val btnBack = findViewById<MaterialButton>(R.id.btnBack)
        val txtTitle = findViewById<TextInputEditText>(R.id.txtTitle)
        val txtAuthor = findViewById<TextInputEditText>(R.id.txtAuthor)
        val txtDescription = findViewById<TextInputEditText>(R.id.txtDescription)
        val btnChooseImage = findViewById<MaterialButton>(R.id.btnChooseImage)
        val btnChoosePdf = findViewById<MaterialButton>(R.id.btnChoosePdf)
        txtPdfName = findViewById<TextView>(R.id.txtPdfName)
        imgBook = findViewById<ImageView>(R.id.imgBook)
        spinnerCategory = findViewById<MaterialAutoCompleteTextView>(R.id.spinnerCategory)
        val btnUpdate = findViewById<MaterialButton>(R.id.btnUpdate)

        val adapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, categories)
        spinnerCategory.setAdapter(adapter)

        // Nhận ID từ Intent và lấy dữ liệu mới nhất từ Firebase
        bookId = intent.getStringExtra("id") ?: ""
        
        if (bookId.isNotEmpty()) {
            firebaseHelper.getBookById(bookId) { book ->
                if (book != null) {
                    currentHiddenStatus = book.isHidden
                    txtTitle.setText(book.title)
                    txtAuthor.setText(book.author)
                    txtDescription.setText(book.description)
                    
                    if (categories.contains(book.category)) {
                        spinnerCategory.setText(book.category, false)
                    } else {
                        spinnerCategory.setText("Khác", false)
                    }

                    if (book.pdfUrl.isNotEmpty()) {
                        if (book.pdfUrl.startsWith("http")) {
                            txtPdfName.text = "PDF: Đã có trên hệ thống"
                        } else {
                            txtPdfName.text = "PDF: Lỗi đường dẫn cũ"
                        }
                    }

                    if (book.image.isNotEmpty()) {
                        if (book.image.startsWith("http")) {
                            com.bumptech.glide.Glide.with(this@EditBookActivity)
                                .load(book.image)
                                .placeholder(R.drawable.white)
                                .into(imgBook)
                        } else {
                            imgBook.setImageResource(R.drawable.white)
                        }
                    }
                }
            }
        }

        btnBack.setOnClickListener {
            val intent = Intent(this, com.example.ngdungocsach.user.MainActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intent)
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

        val pickPdf = registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
            uri?.let {
                try {
                    contentResolver.takePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    selectedPdfUri = it
                    txtPdfName.text = "Đã chọn: " + (it.lastPathSegment ?: "file.pdf")
                } catch (e: Exception) {
                    e.printStackTrace()
                    Toast.makeText(this, "Không thể lấy quyền truy cập file", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnChoosePdf.setOnClickListener {
            pickPdf.launch(arrayOf("application/pdf"))
        }

        btnUpdate.setOnClickListener {
            val updatedTitle = txtTitle.text.toString()
            val updatedAuthor = txtAuthor.text.toString()
            val updatedDescription = txtDescription.text.toString()
            val updatedCategory = spinnerCategory.text.toString()

            if (updatedTitle.isEmpty() || updatedAuthor.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ thông tin", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val progressDialog = android.app.ProgressDialog(this)
            progressDialog.setMessage("Đang cập nhật...")
            progressDialog.setCancelable(false)
            progressDialog.show()

            var finalImageUrl = ""
            var finalPdfUrl = ""
            
            // Lấy URL hiện tại từ Firestore nếu không chọn file mới
            firebaseHelper.getBookById(bookId) { currentBook ->
                finalImageUrl = currentBook?.image ?: ""
                finalPdfUrl = currentBook?.pdfUrl ?: ""

                val uploadsNeeded = mutableListOf<Pair<Uri, String>>()
                // Nếu selectedImageUri thay đổi và khác với cái cũ (đã được parse thành http)
                if (selectedImageUri != null && !selectedImageUri.toString().startsWith("http")) {
                    uploadsNeeded.add(selectedImageUri!! to "book_images")
                }
                if (selectedPdfUri != null && !selectedPdfUri.toString().startsWith("http")) {
                    uploadsNeeded.add(selectedPdfUri!! to "book_pdfs")
                }

                fun performUpdate() {
                    val updatedBook = Book(
                        id = bookId,
                        title = updatedTitle,
                        author = updatedAuthor,
                        image = finalImageUrl,
                        description = updatedDescription,
                        pdfUrl = finalPdfUrl,
                        category = updatedCategory,
                        isHidden = currentHiddenStatus
                    )
                    firebaseHelper.updateBook(updatedBook) { success ->
                        progressDialog.dismiss()
                        if (success) {
                            Toast.makeText(this, "Cập nhật thành công", Toast.LENGTH_SHORT).show()
                            finish()
                        } else {
                            Toast.makeText(this, "Cập nhật thất bại", Toast.LENGTH_SHORT).show()
                        }
                    }
                }

                if (uploadsNeeded.isEmpty()) {
                    performUpdate()
                } else {
                    var completed = 0
                    var isFailed = false
                    uploadsNeeded.forEach { (uri, folder) ->
                        supabaseHelper.uploadFile(uri, folder) { url ->
                            if (isFailed) return@uploadFile
                            completed++
                            if (url != null) {
                                if (folder == "book_images") finalImageUrl = url
                                else finalPdfUrl = url
                                if (completed == uploadsNeeded.size) performUpdate()
                            } else {
                                isFailed = true
                                progressDialog.dismiss()
                                Toast.makeText(this, "Lỗi khi tải file lên Supabase ($folder)", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }
            }
        }
    }
}