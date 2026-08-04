package com.example.ngdungocsach.user

import android.content.Context
import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ngdungocsach.R
import com.example.ngdungocsach.database.FirebaseHelper
import com.github.barteksc.pdfviewer.PDFView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class ReadBookActivity : AppCompatActivity() {

    private lateinit var pdfView: PDFView
    private lateinit var progressBar: ProgressBar
    private lateinit var firebaseHelper: FirebaseHelper
    private var bookId: String = ""
    private var uid: String = ""
    private var userRole: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_read_book)

        firebaseHelper = FirebaseHelper()
        pdfView = findViewById(R.id.pdfView)
        progressBar = findViewById(R.id.progressBar)

        val sharedPreferences = getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        uid = sharedPreferences.getString("uid", "") ?: ""
        userRole = sharedPreferences.getString("role", "") ?: ""

        // Lấy dữ liệu từ Intent
        val pdfUrl = intent.getStringExtra("pdfUrl") ?: ""
        bookId = intent.getStringExtra("bookId") ?: ""
        
        if (pdfUrl.isNotEmpty()) {
            downloadAndShowPDF(pdfUrl)
        } else {
            Toast.makeText(this, "Không tìm thấy link sách", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun downloadAndShowPDF(pdfUrl: String) {
        progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                val url = URL(pdfUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()

                val cachedFile = File(cacheDir, "temp_book.pdf")
                val inputStream = connection.inputStream
                val outputStream = FileOutputStream(cachedFile)

                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }

                outputStream.close()
                inputStream.close()

                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    
                    // Lấy tiến độ cũ trước khi load
                    firebaseHelper.getReadingProgress(uid, bookId) { lastPage ->
                        pdfView.fromFile(cachedFile)
                            .enableSwipe(true)
                            .swipeHorizontal(false)
                            .enableDoubletap(true)
                            .defaultPage(lastPage) // Nhảy đến trang cũ
                            .onPageChange { page, _ ->
                                // Lưu tiến độ mỗi khi chuyển trang (chỉ cho user không phải admin)
                                if (userRole != "admin" && uid.isNotEmpty()) {
                                    firebaseHelper.saveReadingProgress(uid, bookId, page)
                                }
                            }
                            .load()
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this@ReadBookActivity, "Lỗi tải sách: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}
