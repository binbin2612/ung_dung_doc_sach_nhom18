package com.example.ngdungocsach.admin

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import com.example.ngdungocsach.database.DatabaseHelper
import com.example.ngdungocsach.R
import com.example.ngdungocsach.user.MainActivity
import com.example.ngdungocsach.ui.BaseActivity
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.MaterialAutoCompleteTextView

class AdminActivity : BaseActivity() { // Đổi sang BaseActivity

    private lateinit var db: DatabaseHelper
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

        db = DatabaseHelper(this)

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
            val intent = Intent(this, MainActivity::class.java)
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
            val imagePath = selectedImageUri?.toString() ?: ""
            val pdfPath = selectedPdfUri?.toString() ?: ""
            val category = spinnerCategory.text.toString()

            if (title.isNotEmpty() && author.isNotEmpty()) {
                db.addBook(title, author, imagePath, description, pdfPath, category)
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
                Toast.makeText(this, "Vui lòng nhập đủ thông tin", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun getFileName(uri: Uri): String {
        return uri.path?.substringAfterLast('/') ?: "file_pdf"
    }
}
