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
import com.example.ngdungocsach.R
import com.example.ngdungocsach.admin.EditBookActivity
import com.example.ngdungocsach.database.DatabaseHelper
import com.example.ngdungocsach.model.Book
import com.example.ngdungocsach.user.BookDetailActivity
import com.google.android.material.button.MaterialButton

class BookAdapter(private var bookList: MutableList<Book>) :
    RecyclerView.Adapter<BookAdapter.BookViewHolder>() {

    class BookViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgBook: ImageView = itemView.findViewById(R.id.imgBook)
        val tvBookName: TextView = itemView.findViewById(R.id.tvBookName)
        val tvAuthorName: TextView = itemView.findViewById(R.id.tvAuthorName)
        val btnFavorite: MaterialButton = itemView.findViewById(R.id.btnFavorite)
        val btnDelete: MaterialButton = itemView.findViewById(R.id.btnDelete)
        val btnEdit: MaterialButton = itemView.findViewById(R.id.btnEdit)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_book, parent, false)
        return BookViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val book = bookList[position]
        val context = holder.itemView.context
        val db = DatabaseHelper(context)
        val sharedPreferences = context.getSharedPreferences("UserPrefs", Context.MODE_PRIVATE)
        val username = sharedPreferences.getString("username", null)
        val role = sharedPreferences.getString("role", null)

        holder.tvBookName.text = book.title
        holder.tvAuthorName.text = book.author
        
        try {
            holder.imgBook.setImageURI(Uri.parse(book.image))
        } catch (e: Exception) {
            holder.imgBook.setImageResource(R.drawable.book_sample)
        }

        // Xử lý hiển thị dựa trên vai trò
        if (role == "admin") {
            holder.btnDelete.visibility = View.VISIBLE
            holder.btnEdit.visibility = View.VISIBLE
            holder.btnFavorite.visibility = View.GONE
            
            holder.btnDelete.setOnClickListener {
                showDeleteDialog(context, db, book, position)
            }
            
            holder.btnEdit.setOnClickListener {
                val intent = Intent(context, EditBookActivity::class.java)
                intent.putExtra("id", book.id)
                intent.putExtra("title", book.title)
                intent.putExtra("author", book.author)
                intent.putExtra("image", book.image)
                context.startActivity(intent)
            }
        } else if (username != null && role == "user") {
            holder.btnFavorite.visibility = View.VISIBLE
            holder.btnDelete.visibility = View.GONE
            holder.btnEdit.visibility = View.GONE
            
            updateFavoriteIcon(holder.btnFavorite, db.isFavorite(username, book.id))

            holder.btnFavorite.setOnClickListener {
                if (db.isFavorite(username, book.id)) {
                    db.removeFavorite(username, book.id)
                    updateFavoriteIcon(holder.btnFavorite, false)
                } else {
                    db.addFavorite(username, book.id)
                    updateFavoriteIcon(holder.btnFavorite, true)
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

    private fun showDeleteDialog(context: Context, db: DatabaseHelper, book: Book, position: Int) {
        AlertDialog.Builder(context)
            .setTitle("Xóa sách")
            .setMessage("Bạn có chắc chắn muốn xóa cuốn sách '${book.title}' này không?")
            .setPositiveButton("Xóa") { _, _ ->
                if (db.deleteBook(book.id)) {
                    bookList.removeAt(position)
                    notifyItemRemoved(position)
                    notifyItemRangeChanged(position, bookList.size)
                    Toast.makeText(context, "Đã xóa sách", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(context, "Lỗi khi xóa sách", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun updateFavoriteIcon(button: MaterialButton, isFavorite: Boolean) {
        if (isFavorite) {
            button.setIconResource(R.drawable.ic_heart_red)
        } else {
            button.setIconResource(R.drawable.ic_heart_outline)
        }
    }

    override fun getItemCount(): Int = bookList.size
}