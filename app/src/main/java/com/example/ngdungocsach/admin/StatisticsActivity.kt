package com.example.ngdungocsach.admin

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.ngdungocsach.R
import com.example.ngdungocsach.database.FirebaseHelper
import com.example.ngdungocsach.model.Book
import com.example.ngdungocsach.model.Payment
import com.example.ngdungocsach.ui.BaseActivity
import com.google.android.material.button.MaterialButton
import com.google.firebase.firestore.ListenerRegistration
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class StatisticsActivity : BaseActivity() {

    private lateinit var firebaseHelper: FirebaseHelper
    private lateinit var tvTotalBooks: TextView
    private lateinit var tvTotalViews: TextView
    private lateinit var tvTotalRevenue: TextView
    private lateinit var rvTopBooks: RecyclerView
    private lateinit var rvPayments: RecyclerView
    private var booksListener: ListenerRegistration? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_statistics)

        firebaseHelper = FirebaseHelper()
        tvTotalBooks = findViewById(R.id.tvTotalBooks)
        tvTotalViews = findViewById(R.id.tvTotalViews)
        tvTotalRevenue = findViewById(R.id.tvTotalRevenue)
        rvTopBooks = findViewById(R.id.rvTopBooks)
        rvPayments = findViewById(R.id.rvPayments)
        val btnBack = findViewById<MaterialButton>(R.id.btnBack)

        rvTopBooks.layoutManager = LinearLayoutManager(this)
        rvPayments.layoutManager = LinearLayoutManager(this)

        val btnSync = findViewById<MaterialButton>(R.id.btnSync)
        btnSync.setOnClickListener {
            val progressDialog = android.app.ProgressDialog(this)
            progressDialog.setMessage("Đang đồng bộ...")
            progressDialog.show()
            firebaseHelper.syncFavoriteCounts { success ->
                progressDialog.dismiss()
                if (success) {
                    Toast.makeText(this, "Đã đồng bộ thành công", Toast.LENGTH_SHORT).show()
                    loadStatistics()
                } else {
                    Toast.makeText(this, "Đồng bộ thất bại", Toast.LENGTH_SHORT).show()
                }
            }
        }

        btnBack.setOnClickListener { finish() }

        loadStatistics()
    }

    private fun loadStatistics() {
        booksListener?.remove()
        booksListener = firebaseHelper.getAllBooks { books ->
            if (isFinishing || isDestroyed) return@getAllBooks
            tvTotalBooks.text = books.size.toString()
            val totalViews = books.sumOf { it.viewCount }
            tvTotalViews.text = formatCount(totalViews)

            // Sắp xếp theo viewCount để lấy top 10 sách xem nhiều nhất
            val topBooks = books.sortedByDescending { it.viewCount }.take(10)
            rvTopBooks.adapter = TopBookAdapter(topBooks)
        }

        firebaseHelper.getAllPayments { payments ->
            if (isFinishing || isDestroyed) return@getAllPayments
            val totalRevenue = payments.sumOf { it.amount }
            tvTotalRevenue.text = "%,d VNĐ".format(totalRevenue)
            rvPayments.adapter = PaymentAdapter(payments)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        booksListener?.remove()
    }

    private fun formatCount(count: Int): String {
        return if (count >= 1000) {
            String.format("%.1fk", count / 1000.0)
        } else {
            count.toString()
        }
    }

    class TopBookAdapter(private val books: List<Book>) : RecyclerView.Adapter<TopBookAdapter.ViewHolder>() {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvRank: TextView = view.findViewById(R.id.tvRank)
            val imgBook: ImageView = view.findViewById(R.id.imgBook)
            val tvTitle: TextView = view.findViewById(R.id.tvTitle)
            val tvAuthor: TextView = view.findViewById(R.id.tvAuthor)
            val tvFavoriteCount: TextView = view.findViewById(R.id.tvFavoriteCount)
            val tvViewCount: TextView = view.findViewById(R.id.tvViewCount)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_top_book, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val book = books[position]
            holder.tvRank.text = (position + 1).toString()
            holder.tvTitle.text = book.title
            holder.tvAuthor.text = book.author
            holder.tvFavoriteCount.text = "${book.favoriteCount} ❤️"
            holder.tvViewCount.text = "${book.viewCount} 👁️"

            if (book.image.isNotEmpty()) {
                Glide.with(holder.itemView.context)
                    .load(book.image)
                    .placeholder(R.drawable.white)
                    .into(holder.imgBook)
            } else {
                holder.imgBook.setImageResource(R.drawable.white)
            }
        }

        override fun getItemCount() = books.size
    }

    class PaymentAdapter(private val payments: List<Payment>) : RecyclerView.Adapter<PaymentAdapter.ViewHolder>() {
        class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
            val tvUser: TextView = view.findViewById(R.id.tvPaymentUser)
            val tvDate: TextView = view.findViewById(R.id.tvPaymentDate)
            val tvPackage: TextView = view.findViewById(R.id.tvPaymentPackage)
            val tvAmount: TextView = view.findViewById(R.id.tvPaymentAmount)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_payment, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val payment = payments[position]
            holder.tvUser.text = "User: ${payment.username}"
            val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
            holder.tvDate.text = sdf.format(Date(payment.timestamp))
            holder.tvPackage.text = payment.packageName
            holder.tvAmount.text = "%,d VNĐ".format(payment.amount)
        }

        override fun getItemCount() = payments.size
    }
}
