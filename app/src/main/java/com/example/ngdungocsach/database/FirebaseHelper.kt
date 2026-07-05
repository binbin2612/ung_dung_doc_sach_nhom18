package com.example.ngdungocsach.database

import android.util.Log
import com.example.ngdungocsach.model.Book
import com.example.ngdungocsach.model.User
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.toObject
import android.net.Uri
import com.google.firebase.auth.FirebaseAuth

class FirebaseHelper {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val usersRef = db.collection("users")
    private val booksRef = db.collection("books")
    private val favoritesRef = db.collection("favorites")

    init {
        seedData()
    }

    private fun seedData() {
        // Đảm bảo Admin mặc định luôn tồn tại
        val adminData = hashMapOf(
            "uid" to "admin",
            "username" to "admin",
            "password" to "admin",
            "role" to "admin",
            "subscriptionExpiry" to 0L
        )
        usersRef.document("admin").set(adminData, SetOptions.merge())
    }

    // --- XỬ LÝ TÀI KHOẢN ---
    fun login(username: String, password: String, onResult: (String?, String) -> Unit) {
        val cleanUser = username.lowercase().trim()
        usersRef.document(cleanUser).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                if (doc.getBoolean("isBlocked") == true) {
                    onResult(null, "Tài khoản của bạn đã bị khóa bởi Admin")
                    return@addOnSuccessListener
                }
                if (doc.getString("password") == password) {
                    onResult(doc.getString("role"), "Đăng nhập thành công")
                } else onResult(null, "Sai mật khẩu")
            } else {
                if (cleanUser == "admin") {
                    seedData() // Tự phục hồi admin
                    onResult(null, "Đã khởi tạo lại Admin, vui lòng thử lại")
                } else onResult(null, "Tài khoản không tồn tại")
            }
        }.addOnFailureListener { onResult(null, "Lỗi kết nối mạng") }
    }

    fun register(user: User, password: String, onResult: (Boolean) -> Unit) {
        val cleanUser = user.username.lowercase().trim()
        val data = hashMapOf(
            "uid" to cleanUser,
            "username" to cleanUser,
            "password" to password,
            "role" to user.role,
            "subscriptionExpiry" to user.subscriptionExpiry,
            "isBlocked" to user.isBlocked,
            "isHidden" to user.isHidden
        )
        usersRef.document(cleanUser).set(data).addOnCompleteListener { onResult(it.isSuccessful) }
    }

    fun checkUserExists(username: String, onResult: (Boolean) -> Unit) {
        usersRef.document(username.lowercase().trim()).get().addOnSuccessListener { onResult(it.exists()) }
    }

    // --- QUẢN LÝ SÁCH ---
    fun getAllBooks(onResult: (List<Book>) -> Unit) {
        booksRef.get().addOnSuccessListener { res ->
            onResult(res.map { it.toObject<Book>().copy(id = it.id) })
        }
    }

    fun getBookById(id: String, onResult: (Book?) -> Unit) {
        booksRef.document(id).get().addOnSuccessListener { onResult(it.toObject<Book>()?.copy(id = it.id)) }
    }

    fun addBook(book: Book, onResult: (Boolean) -> Unit) {
        val data = hashMapOf(
            "title" to book.title, "author" to book.author, "category" to book.category,
            "description" to book.description, "image" to book.image, "pdfUrl" to book.pdfUrl,
            "isHidden" to book.isHidden
        )
        booksRef.add(data).addOnCompleteListener { onResult(it.isSuccessful) }
    }

    fun updateBook(book: Book, onResult: (Boolean) -> Unit) {
        val data = hashMapOf(
            "title" to book.title, "author" to book.author, "category" to book.category,
            "description" to book.description, "image" to book.image, "pdfUrl" to book.pdfUrl,
            "isHidden" to book.isHidden
        )
        booksRef.document(book.id).update(data as Map<String, Any>).addOnCompleteListener { onResult(it.isSuccessful) }
    }

    fun deleteBook(id: String, onResult: (Boolean) -> Unit) {
        booksRef.document(id).delete().addOnSuccessListener {
            // Xóa các bản ghi yêu thích liên quan
            favoritesRef.whereEqualTo("bookId", id).get().addOnSuccessListener { res ->
                val batch = db.batch()
                res.forEach { batch.delete(it.reference) }
                batch.commit()
            }
            onResult(true)
        }.addOnFailureListener { onResult(false) }
    }

    // --- YÊU THÍCH (TỐI ƯU HÓA TRUY VẤN) ---
    fun toggleFavorite(username: String, bookId: String, onResult: (Boolean) -> Unit) {
        val id = "${username.lowercase().trim()}_$bookId"
        favoritesRef.document(id).get().addOnSuccessListener { doc ->
            if (doc.exists()) favoritesRef.document(id).delete() else favoritesRef.document(id).set(mapOf("username" to username.lowercase().trim(), "bookId" to bookId))
            onResult(true)
        }
    }

    fun isFavorite(username: String, bookId: String, onResult: (Boolean) -> Unit) {
        favoritesRef.document("${username.lowercase().trim()}_$bookId").get().addOnSuccessListener { onResult(it.exists()) }
    }

    fun getFavoriteBooks(username: String, onResult: (List<Book>) -> Unit) {
        favoritesRef.whereEqualTo("username", username.lowercase().trim()).get().addOnSuccessListener { res ->
            val ids = res.mapNotNull { it.getString("bookId") }
            if (ids.isEmpty()) { onResult(emptyList()); return@addOnSuccessListener }
            
            // Xử lý chunk 30 ids cho Firestore whereIn
            val chunks = ids.chunked(30)
            val resultBooks = mutableListOf<Book>()
            var completed = 0
            chunks.forEach { chunk ->
                booksRef.whereIn(FieldPath.documentId(), chunk).get().addOnSuccessListener { bRes ->
                    resultBooks.addAll(bRes.map { it.toObject<Book>().copy(id = it.id) })
                    if (++completed == chunks.size) onResult(resultBooks)
                }
            }
        }
    }

    // --- GÓI CƯỚC ---
    fun getSubscriptionExpiry(username: String, onResult: (Long) -> Unit) {
        usersRef.document(username.lowercase().trim()).get().addOnSuccessListener { onResult(it.getLong("subscriptionExpiry") ?: 0L) }
    }

    fun updateSubscription(username: String, days: Int, onResult: (Boolean) -> Unit) {
        val user = username.lowercase().trim()
        usersRef.document(user).get().addOnSuccessListener { doc ->
            val now = System.currentTimeMillis()
            val current = doc.getLong("subscriptionExpiry") ?: 0L
            val start = if (current > now) current else now
            val newExpiry = start + (days.toLong() * 24 * 60 * 60 * 1000)
            usersRef.document(user).update("subscriptionExpiry", newExpiry).addOnCompleteListener { onResult(it.isSuccessful) }
        }
    }

    fun cancelSubscription(username: String, onResult: (Boolean) -> Unit) {
        usersRef.document(username.lowercase().trim()).update("subscriptionExpiry", 0L).addOnCompleteListener { onResult(it.isSuccessful) }
    }

    // --- QUẢN LÝ TÀI KHOẢN ---
    fun getAllAccounts(onResult: (List<User>) -> Unit) {
        usersRef.get().addOnSuccessListener { res ->
            val users = res.mapNotNull { doc ->
                val user = doc.toObject<User>()
                user.uid = doc.id
                Log.d("FirebaseHelper", "User: ${user.username}, isHidden: ${user.isHidden}, isBlocked: ${user.isBlocked}")
                user
            }
            onResult(users)
        }.addOnFailureListener { e ->
            Log.e("FirebaseHelper", "Error getting accounts", e)
            onResult(emptyList())
        }
    }

    fun deleteAccount(username: String, onResult: (Boolean) -> Unit) {
        usersRef.document(username.lowercase().trim()).delete().addOnCompleteListener { onResult(it.isSuccessful) }
    }

    fun toggleBlockUser(username: String, isBlocked: Boolean, onResult: (Boolean) -> Unit) {
        usersRef.document(username.lowercase().trim()).update("isBlocked", isBlocked)
            .addOnCompleteListener { onResult(it.isSuccessful) }
    }

    fun toggleHideUser(username: String, isHidden: Boolean, onResult: (Boolean) -> Unit) {
        usersRef.document(username.lowercase().trim()).update("isHidden", isHidden)
            .addOnCompleteListener { onResult(it.isSuccessful) }
    }

    fun toggleHideBook(bookId: String, isHidden: Boolean, onResult: (Boolean) -> Unit) {
        booksRef.document(bookId).update("isHidden", isHidden)
            .addOnCompleteListener { onResult(it.isSuccessful) }
    }


    fun cleanInvalidUris(onComplete: (Int) -> Unit) {
        booksRef.get().addOnSuccessListener { snapshot ->
            val batch = db.batch()
            var count = 0
            for (doc in snapshot.documents) {
                val image = doc.getString("image") ?: ""
                val pdfUrl = doc.getString("pdfUrl") ?: ""
                var changed = false
                val updates = mutableMapOf<String, Any>()

                if (image.startsWith("content://")) {
                    updates["image"] = ""
                    changed = true
                }
                if (pdfUrl.startsWith("content://")) {
                    updates["pdfUrl"] = ""
                    changed = true
                }

                if (changed) {
                    batch.update(doc.reference, updates)
                    count++
                }
            }
            if (count > 0) {
                batch.commit().addOnCompleteListener { onComplete(count) }
            } else {
                onComplete(0)
            }
        }
    }

    // --- FAVORITES LEGACY WRAPPERS ---
    fun addFavorite(username: String, bookId: String, onResult: (Boolean) -> Unit) {
        val id = "${username.lowercase().trim()}_$bookId"
        favoritesRef.document(id).set(mapOf("username" to username.lowercase().trim(), "bookId" to bookId))
            .addOnCompleteListener { onResult(it.isSuccessful) }
    }

    fun removeFavorite(username: String, bookId: String, onResult: (Boolean) -> Unit) {
        val id = "${username.lowercase().trim()}_$bookId"
        favoritesRef.document(id).delete().addOnCompleteListener { onResult(it.isSuccessful) }
    }
}
