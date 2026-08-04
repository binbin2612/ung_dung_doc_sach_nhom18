package com.example.ngdungocsach.adapter

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import android.util.Log
import com.example.ngdungocsach.R
import com.example.ngdungocsach.admin.EditBookActivity
import com.example.ngdungocsach.database.FirebaseHelper
import com.example.ngdungocsach.model.Book
import com.example.ngdungocsach.user.BookDetailActivity
import com.google.android.material.button.MaterialButton
import com.bumptech.glide.Glide

class BookAdapter(private var bookList: MutableList<Book>) :
    RecyclerView.Adapter<BookAdapter.BookViewHolder>() {

    private val firebaseHelper = FirebaseHelper()

    class BookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgBook: ImageView = itemView.findViewById(R.id.imgBook)
        val tvBookName: TextView = itemView.findViewById(R.id.tvBookName)
        val tvAuthorName: TextView = itemView.findViewById(R.id.tvAuthorName)
        val tvCategoryName: TextView = itemView.findViewById(R.id.tvCategoryName)
        val btnFavorite: MaterialButton = itemView.findViewById(R.id.btnFavorite)
        val btnDelete: MaterialButton = itemView.findViewById(R.id.btnDelete)
        val btnEdit: MaterialButton = itemView.findViewById(R.id.btnEdit)
        val btnHide: MaterialButton = itemView.findViewById(R.id.btnHide)
        val tvProgress: TextView = itemView.findViewById(R.id.tvProgress)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_book, parent, false)
        return BookViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val book = bookList[position]
        val context = holder.itemView.context
        val firebaseHelper = FirebaseHelper()
        val sharedPreferences = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val uid = sharedPreferences.getString("uid", null)
        val role = sharedPreferences.getString("role", null)

        holder.tvBookName.text = book.title
        holder.tvAuthorName.text = book.author
        holder.tvCategoryName.text = book.category
        
        // Hiển thị tiến độ đọc
        if (uid != null) {
            firebaseHelper.getReadingProgress(uid, book.id) { lastPage ->
                if (lastPage > 0) {
                    holder.tvProgress.visibility = View.VISIBLE
                    holder.tvProgress.text = "Đã đọc: Trang ${lastPage + 1}"
                } else {
                    holder.tvProgress.visibility = View.GONE
                }
            }
        } else {
            holder.tvProgress.visibility = View.GONE
        }
        
        if (book.image.isNotEmpty() && book.image.startsWith("http")) {
            Glide.with(context)
                .load(book.image)
                .placeholder(R.drawable.white)
                .error(R.drawable.white)
                .into(holder.imgBook)
        } else {
            holder.imgBook.setImageResource(R.drawable.white)
            if (book.image.startsWith("content://")) {
                Log.w("BookAdapter", "Local URI detected in database: ${book.image}")
            }
        }

        // Xử lý hiển thị dựa trên vai trò
        if (role == "admin") {
            holder.btnDelete.visibility = View.VISIBLE
            holder.btnEdit.visibility = View.VISIBLE
            holder.btnHide.visibility = View.VISIBLE
            holder.btnFavorite.visibility = View.GONE

            updateHideIcon(holder.btnHide, book.isHidden)

            holder.btnHide.setOnClickListener {
                val newHideStatus = !book.isHidden
                firebaseHelper.toggleHideBook(book.id, newHideStatus) { success ->
                    if (success) {
                        book.isHidden = newHideStatus
                        updateHideIcon(holder.btnHide, book.isHidden)
                        val msg = if (newHideStatus) "Đã ẩn sách" else "Đã hiện sách"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                    }
                }
            }
            
            holder.btnDelete.setOnClickListener {
                showDeleteDialog(context, firebaseHelper, book, position)
            }
            
            holder.btnEdit.setOnClickListener {
                val intent = Intent(context, EditBookActivity::class.java)
                intent.putExtra("id", book.id)
                intent.putExtra("title", book.title)
                intent.putExtra("author", book.author)
                intent.putExtra("image", book.image)
                intent.putExtra("description", book.description)
                intent.putExtra("pdfUrl", book.pdfUrl)
                intent.putExtra("category", book.category)
                context.startActivity(intent)
            }
        } else if (uid != null && role == "user") {
            holder.btnFavorite.visibility = View.VISIBLE
            holder.btnDelete.visibility = View.GONE
            holder.btnEdit.visibility = View.GONE
            
            firebaseHelper.isFavorite(uid, book.id) { isFav ->
                updateFavoriteIcon(holder.btnFavorite, isFav)
            }

            holder.btnFavorite.setOnClickListener {
                firebaseHelper.isFavorite(uid, book.id) { isFav ->
                    if (isFav) {
                        firebaseHelper.removeFavorite(uid, book.id) { success ->
                            if (success) updateFavoriteIcon(holder.btnFavorite, false)
                        }
                    } else {
                        firebaseHelper.addFavorite(uid, book.id) { success ->
                            if (success) updateFavoriteIcon(holder.btnFavorite, true)
                        }
                    }
                }
            }
        } else {
            holder.btnFavorite.visibility = View.GONE
            holder.btnDelete.visibility = View.GONE
            holder.btnEdit.visibility = View.GONE
        }

        holder.itemView.setOnClickListener {
            val intent = Intent(context, BookDetailActivity::class.java)
            intent.putExtra("id", book.id)
            intent.putExtra("title", book.title)
            intent.putExtra("author", book.author)
            intent.putExtra("image", book.image)
            context.startActivity(intent)
        }
    }

    private fun showDeleteDialog(context: Context, firebaseHelper: FirebaseHelper, book: Book, position: Int) {
        AlertDialog.Builder(context)
            .setTitle("Xóa sách")
            .setMessage("Bạn có chắc chắn muốn xóa cuốn sách '${book.title}' này không?")
            .setPositiveButton("Xóa") { _, _ ->
                firebaseHelper.deleteBook(book.id) { success ->
                    if (success) {
                        bookList.removeAt(position)
                        notifyItemRemoved(position)
                        notifyItemRangeChanged(position, bookList.size)
                        Toast.makeText(context, "Đã xóa sách", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Lỗi khi xóa sách", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun updateHideIcon(button: MaterialButton, isHidden: Boolean) {
        button.alpha = 1.0f 
        if (isHidden) {
            button.setIconResource(R.drawable.ic_show)
            button.setIconTintResource(R.color.primary) // Hiện màu xanh rõ nét khi đang ẩn
        } else {
            button.setIconResource(R.drawable.ic_hide)
            button.setIconTintResource(R.color.white) // Trắng 100%
        }
    }

    private fun updateFavoriteIcon(button: MaterialButton, isFavorite: Boolean) {
        button.alpha = 1.0f
        if (isFavorite) {
            button.setIconResource(R.drawable.ic_heart_red)
            button.iconTint = null // Giữ nguyên màu đỏ gốc của file vector
        } else {
            button.setIconResource(R.drawable.ic_heart_outline)
            button.setIconTintResource(R.color.white) // Trắng 100%
        }
    }

    override fun getItemCount(): Int = bookList.size
}
