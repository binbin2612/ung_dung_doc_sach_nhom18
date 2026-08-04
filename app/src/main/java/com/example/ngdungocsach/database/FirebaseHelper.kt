package com.example.ngdungocsach.database

import android.util.Log
import com.example.ngdungocsach.model.Book
import com.example.ngdungocsach.model.User
import com.google.firebase.firestore.FieldPath
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ListenerRegistration
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.toObject
import android.net.Uri
import com.google.firebase.auth.AuthCredential
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser

class FirebaseHelper {
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private val usersRef = db.collection("users")
    private val booksRef = db.collection("books")
    private val favoritesRef = db.collection("favorites")
    private val readingProgressRef = db.collection("reading_progress")
    private val viewsRef = db.collection("book_views")
    private val paymentsRef = db.collection("payments")
    private val settingsRef = db.collection("settings")

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

    // --- XỬ LÝ TÀI KHOẢN (FIREBASE AUTH) ---
    fun getCurrentUser(): FirebaseUser? = auth.currentUser

    fun registerWithAuth(email: String, password: String, username: String, onResult: (Boolean, String) -> Unit) {
        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val firebaseUser = auth.currentUser
                firebaseUser?.let {
                    val uid = it.uid
                    val userData = hashMapOf(
                        "uid" to uid,
                        "username" to username,
                        "email" to email,
                        "role" to "user",
                        "subscriptionExpiry" to 0L,
                        "isBlocked" to false,
                        "isHidden" to false
                    )
                    usersRef.document(uid).set(userData).addOnCompleteListener { dbTask ->
                        if (dbTask.isSuccessful) onResult(true, "Đăng ký thành công")
                        else onResult(false, "Lỗi lưu thông tin người dùng")
                    }
                }
            } else {
                onResult(false, task.exception?.message ?: "Đăng ký thất bại")
            }
        }
    }

    fun loginWithAuth(email: String, password: String, onResult: (String?, String) -> Unit) {
        auth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val uid = auth.currentUser?.uid ?: ""
                getUserData(uid) { user ->
                    if (user != null) {
                        if (user.isBlocked) {
                            auth.signOut()
                            onResult(null, "Tài khoản của bạn đã bị khóa")
                        } else {
                            onResult(user.role, "Đăng nhập thành công")
                        }
                    } else {
                        onResult(null, "Không tìm thấy dữ liệu người dùng")
                    }
                }
            } else {
                onResult(null, task.exception?.message ?: "Đăng nhập thất bại")
            }
        }
    }

    fun signInWithCredential(credential: AuthCredential, onResult: (Boolean, String) -> Unit) {
        auth.signInWithCredential(credential).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = auth.currentUser
                user?.let {
                    checkAndCreateUserInFirestore(it) { success ->
                        onResult(success, if (success) "Đăng nhập thành công" else "Lỗi tạo thông tin người dùng")
                    }
                }
            } else {
                onResult(false, task.exception?.message ?: "Đăng nhập thất bại")
            }
        }
    }

    private fun checkAndCreateUserInFirestore(firebaseUser: FirebaseUser, onResult: (Boolean) -> Unit) {
        val uid = firebaseUser.uid
        usersRef.document(uid).get().addOnSuccessListener { doc ->
            if (!doc.exists()) {
                val newUser = hashMapOf(
                    "uid" to uid,
                    "username" to (firebaseUser.displayName ?: firebaseUser.email ?: firebaseUser.phoneNumber ?: "User"),
                    "email" to firebaseUser.email,
                    "phoneNumber" to firebaseUser.phoneNumber,
                    "role" to "user",
                    "subscriptionExpiry" to 0L,
                    "isBlocked" to false,
                    "isHidden" to false
                )
                usersRef.document(uid).set(newUser).addOnCompleteListener { onResult(it.isSuccessful) }
            } else {
                onResult(true)
            }
        }.addOnFailureListener { onResult(false) }
    }

    fun getUserData(uid: String, onResult: (User?) -> Unit) {
        usersRef.document(uid).get().addOnSuccessListener { doc ->
            onResult(doc.toObject<User>()?.apply { this.uid = doc.id })
        }.addOnFailureListener { onResult(null) }
    }

    fun login(username: String, password: String, onResult: (String?, String) -> Unit) {
        val cleanUser = username.lowercase().trim()
        
        // Nếu chưa có phiên đăng nhập nào, dùng đăng nhập ẩn để có quyền đọc Firestore Rules
        if (auth.currentUser == null) {
            auth.signInAnonymously().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    performLegacyLogin(cleanUser, password, onResult)
                } else {
                    onResult(null, "Lỗi khởi tạo hệ thống: ${task.exception?.message}")
                }
            }
        } else {
            performLegacyLogin(cleanUser, password, onResult)
        }
    }

    private fun performLegacyLogin(username: String, password: String, onResult: (String?, String) -> Unit) {
        usersRef.document(username).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                val isBlocked = doc.getBoolean("isBlocked") ?: false
                if (isBlocked) {
                    onResult(null, "Tài khoản này đã bị khóa")
                    return@addOnSuccessListener
                }

                val dbPass = doc.getString("password")
                if (dbPass == password) {
                    onResult(doc.getString("role") ?: "user", "Đăng nhập thành công")
                } else {
                    onResult(null, "Sai mật khẩu")
                }
            } else {
                if (username == "admin") {
                    seedData() // Tạo lại admin nếu mất
                    if (password == "admin") onResult("admin", "Khởi tạo Admin thành công")
                    else onResult(null, "Mật khẩu Admin mặc định là 'admin'")
                } else {
                    onResult(null, "Tài khoản không tồn tại")
                }
            }
        }.addOnFailureListener { e ->
            onResult(null, "Lỗi cơ sở dữ liệu: ${e.message}")
        }
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
    fun incrementViewCount(uid: String?, bookId: String) {
        if (uid == null || uid.isEmpty() || uid.lowercase() == "admin") return

        val cleanUid = uid.lowercase().trim()
        val viewId = "${cleanUid}_$bookId"

        viewsRef.document(viewId).get().addOnSuccessListener { doc ->
            if (!doc.exists()) {
                val data = hashMapOf(
                    "uid" to cleanUid,
                    "bookId" to bookId,
                    "timestamp" to System.currentTimeMillis()
                )
                viewsRef.document(viewId).set(data).addOnSuccessListener {
                    booksRef.document(bookId).update("viewCount", FieldValue.increment(1))
                }
            }
        }
    }

    fun getAllBooks(onResult: (List<Book>) -> Unit): ListenerRegistration {
        return booksRef.addSnapshotListener { res, _ ->
            if (res != null) {
                onResult(res.map { it.toObject<Book>().copy(id = it.id) })
            }
        }
    }

    fun getBookById(id: String, onResult: (Book?) -> Unit): ListenerRegistration {
        return booksRef.document(id).addSnapshotListener { snapshot, _ ->
            onResult(snapshot?.toObject<Book>()?.copy(id = snapshot.id))
        }
    }

    fun getBookByIdOnce(id: String, onResult: (Book?) -> Unit) {
        booksRef.document(id).get().addOnSuccessListener { snapshot ->
            onResult(snapshot?.toObject<Book>()?.copy(id = snapshot.id))
        }.addOnFailureListener { onResult(null) }
    }

    fun addBook(book: Book, onResult: (Boolean) -> Unit) {
        val data = hashMapOf(
            "title" to book.title, "author" to book.author, "category" to book.category,
            "description" to book.description, "image" to book.image, "pdfUrl" to book.pdfUrl,
            "isHidden" to book.isHidden, "viewCount" to 0, "favoriteCount" to 0
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
    fun toggleFavorite(uid: String, bookId: String, onResult: (Boolean) -> Unit) {
        val id = "${uid.lowercase().trim()}_$bookId"
        favoritesRef.document(id).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                removeFavorite(uid, bookId) { onResult(it) }
            } else {
                addFavorite(uid, bookId) { onResult(it) }
            }
        }
    }

    fun isFavorite(uid: String, bookId: String, onResult: (Boolean) -> Unit) {
        favoritesRef.document("${uid.lowercase().trim()}_$bookId").get().addOnSuccessListener { onResult(it.exists()) }
    }

    fun getFavoriteBooks(uid: String, onResult: (List<Book>) -> Unit) {
        favoritesRef.whereEqualTo("uid", uid.lowercase().trim()).get().addOnSuccessListener { res ->
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
    fun getSubscriptionExpiry(uid: String, onResult: (Long) -> Unit) {
        usersRef.document(uid).get().addOnSuccessListener { onResult(it.getLong("subscriptionExpiry") ?: 0L) }
    }

    fun updateSubscription(uid: String, days: Int, onResult: (Boolean) -> Unit) {
        val userDoc = usersRef.document(uid)
        userDoc.get().addOnSuccessListener { doc ->
            val now = System.currentTimeMillis()
            val current = doc.getLong("subscriptionExpiry") ?: 0L
            val start = if (current > now) current else now
            val newExpiry = start + (days.toLong() * 24 * 60 * 60 * 1000)
            
            if (doc.exists()) {
                userDoc.update("subscriptionExpiry", newExpiry).addOnCompleteListener { onResult(it.isSuccessful) }
            } else {
                // Nếu là tài khoản legacy dùng username làm ID
                onResult(false)
            }
        }.addOnFailureListener { onResult(false) }
    }

    fun cancelSubscription(uid: String, onResult: (Boolean) -> Unit) {
        usersRef.document(uid).update("subscriptionExpiry", 0L).addOnCompleteListener { onResult(it.isSuccessful) }
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

    fun deleteAccount(uid: String, onResult: (Boolean) -> Unit) {
        usersRef.document(uid).delete().addOnCompleteListener { onResult(it.isSuccessful) }
    }

    fun toggleBlockUser(uid: String, isBlocked: Boolean, onResult: (Boolean) -> Unit) {
        usersRef.document(uid).update("isBlocked", isBlocked)
            .addOnCompleteListener { onResult(it.isSuccessful) }
    }

    fun toggleHideUser(uid: String, isHidden: Boolean, onResult: (Boolean) -> Unit) {
        usersRef.document(uid).update("isHidden", isHidden)
            .addOnCompleteListener { onResult(it.isSuccessful) }
    }

    fun toggleHideBook(bookId: String, isHidden: Boolean, onResult: (Boolean) -> Unit) {
        booksRef.document(bookId).update("isHidden", isHidden)
            .addOnCompleteListener { onResult(it.isSuccessful) }
    }

    fun updateUsername(uid: String, newUsername: String, onResult: (Boolean) -> Unit) {
        usersRef.document(uid).update("username", newUsername)
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
    fun addFavorite(uid: String, bookId: String, onResult: (Boolean) -> Unit) {
        val id = "${uid.lowercase().trim()}_$bookId"
        favoritesRef.document(id).set(mapOf("uid" to uid.lowercase().trim(), "bookId" to bookId))
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    booksRef.document(bookId).update("favoriteCount", FieldValue.increment(1))
                }
                onResult(task.isSuccessful)
            }
    }

    fun removeFavorite(uid: String, bookId: String, onResult: (Boolean) -> Unit) {
        val id = "${uid.lowercase().trim()}_$bookId"
        favoritesRef.document(id).delete().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                booksRef.document(bookId).update("favoriteCount", FieldValue.increment(-1))
            }
            onResult(task.isSuccessful)
        }
    }

    // --- TIẾN ĐỘ ĐỌC SÁCH ---
    fun saveReadingProgress(uid: String, bookId: String, page: Int) {
        val id = "${uid.lowercase().trim()}_$bookId"
        val data = hashMapOf(
            "uid" to uid.lowercase().trim(),
            "bookId" to bookId,
            "lastPage" to page,
            "timestamp" to System.currentTimeMillis()
        )
        readingProgressRef.document(id).set(data, SetOptions.merge())
    }

    fun syncFavoriteCounts(onComplete: (Boolean) -> Unit) {
        booksRef.get().addOnSuccessListener { booksSnapshot ->
            val totalBooks = booksSnapshot.size()
            if (totalBooks == 0) {
                onComplete(true)
                return@addOnSuccessListener
            }

            var processed = 0
            for (bookDoc in booksSnapshot.documents) {
                val bookId = bookDoc.id
                
                // Đếm favorite
                favoritesRef.whereEqualTo("bookId", bookId).get().addOnSuccessListener { favsSnapshot ->
                    val favCount = favsSnapshot.size()
                    
                    // Đếm view thực tế từ collection book_views
                    viewsRef.whereEqualTo("bookId", bookId).get().addOnSuccessListener { viewsSnapshot ->
                        val viewCount = viewsSnapshot.size()
                        
                        booksRef.document(bookId).update(
                            "favoriteCount", favCount,
                            "viewCount", viewCount
                        ).addOnCompleteListener {
                            processed++
                            if (processed == totalBooks) {
                                onComplete(true)
                            }
                        }
                    }
                }
            }
        }
    }

    fun getReadingProgress(uid: String, bookId: String, onResult: (Int) -> Unit) {
        val id = "${uid.lowercase().trim()}_$bookId"
        readingProgressRef.document(id).get().addOnSuccessListener { doc ->
            if (doc.exists()) {
                onResult(doc.getLong("lastPage")?.toInt() ?: 0)
            } else {
                onResult(0)
            }
        }.addOnFailureListener { onResult(0) }
    }

    // --- THANH TOÁN ---
    fun savePayment(payment: com.example.ngdungocsach.model.Payment, onResult: (Boolean) -> Unit) {
        paymentsRef.add(payment).addOnCompleteListener { onResult(it.isSuccessful) }
    }

    fun getAllPayments(onResult: (List<com.example.ngdungocsach.model.Payment>) -> Unit) {
        paymentsRef.orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
            .get().addOnSuccessListener { res ->
                onResult(res.map { it.toObject<com.example.ngdungocsach.model.Payment>().apply { id = it.id } })
            }.addOnFailureListener { onResult(emptyList()) }
    }

    // --- CÀI ĐẶT HỆ THỐNG ---
    fun getPaymentSettings(onResult: (Map<String, Any>?) -> Unit) {
        settingsRef.document("payment_config").get().addOnSuccessListener { doc ->
            onResult(doc.data)
        }.addOnFailureListener { onResult(null) }
    }

    fun updatePaymentSettings(settings: Map<String, Any>, onResult: (Boolean) -> Unit) {
        settingsRef.document("payment_config").set(settings, SetOptions.merge())
            .addOnCompleteListener { onResult(it.isSuccessful) }
    }
}
