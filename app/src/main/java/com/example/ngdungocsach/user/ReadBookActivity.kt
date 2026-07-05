package com.example.ngdungocsach.user

import android.os.Bundle
import android.view.View
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.ngdungocsach.R
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_read_book)

        pdfView = findViewById(R.id.pdfView)
        progressBar = findViewById(R.id.progressBar)

        // Lấy đường link PDF từ Intent (được truyền từ BookDetailActivity)
        val pdfUrl = intent.getStringExtra("pdfUrl") ?: ""
        
        if (pdfUrl.isNotEmpty()) {
            downloadAndShowPDF(pdfUrl)
        } else {
            Toast.makeText(this, "Không tìm thấy link sách", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun downloadAndShowPDF(pdfUrl: String) {
        // Hiện thanh loading
        progressBar.visibility = View.VISIBLE

        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 1. Kết nối tới link Supabase/Firebase
                val url = URL(pdfUrl)
                val connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connect()

                // 2. Tạo một file tạm trong bộ nhớ Cache của App
                val cachedFile = File(cacheDir, "temp_book.pdf")
                
                val inputStream = connection.inputStream
                val outputStream = FileOutputStream(cachedFile)

                // 3. Tiến hành tải và lưu ngầm
                val buffer = ByteArray(8192)
                var bytesRead: Int
                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                }

                outputStream.close()
                inputStream.close()

                // 4. Chuyển sang luồng UI chính để hiển thị sách
                withContext(Dispatchers.Main) {
                    progressBar.visibility = View.GONE
                    
                    pdfView.fromFile(cachedFile)
                        .enableSwipe(true)
                        .swipeHorizontal(false)
                        .enableDoubletap(true)
                        .defaultPage(0)
                        .load()
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
