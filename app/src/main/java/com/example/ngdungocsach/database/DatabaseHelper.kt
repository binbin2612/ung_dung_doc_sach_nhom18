package com.example.ngdungocsach.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.example.ngdungocsach.model.Book

class DatabaseHelper(context: Context) :
    SQLiteOpenHelper(context, "BookDB", null, 6) { // Version 6: Thêm cột pdf_url

    override fun onCreate(db: SQLiteDatabase) {

        // bảng account
        db.execSQL(
            "CREATE TABLE account(" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "username TEXT UNIQUE," +
                    "password TEXT," +
                    "role TEXT)"
        )

        // bảng book
        db.execSQL(
            "CREATE TABLE book(" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "title TEXT," +
                    "author TEXT," +
                    "image TEXT," +
                    "description TEXT DEFAULT ''," +
                    "pdf_url TEXT DEFAULT '')"
        )

        // bảng favorite
        db.execSQL(
            "CREATE TABLE favorite(" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "username TEXT," +
                    "book_id INTEGER," +
                    "UNIQUE(username, book_id))"
        )

        // tạo admin mặc định
        val cvAdmin = ContentValues()
        cvAdmin.put("username", "admin")
        cvAdmin.put("password", "123")
        cvAdmin.put("role", "admin")
        db.insert("account", null, cvAdmin)

        // Thêm dữ liệu sách mẫu
        insertSampleBooks(db)
    }

    private fun insertSampleBooks(db: SQLiteDatabase) {
        val books = arrayOf(
            arrayOf("Đắc Nhân Tâm", "Dale Carnegie"),
            arrayOf("Nhà Giả Kim", "Paulo Coelho"),
            arrayOf("Tôi Thấy Hoa Vàng Trên Cỏ Xanh", "Nguyễn Nhật Ánh"),
            arrayOf("Số Đỏ", "Vũ Trọng Phụng"),
            arrayOf("Lão Hạc", "Nam Cao")
        )

        for (book in books) {
            val cv = ContentValues()
            cv.put("title", book[0])
            cv.put("author", book[1])
            cv.put("image", "")
            cv.put("description", "Nội dung mô tả cho cuốn ${book[0]} đang được cập nhật...")
            cv.put("pdf_url", "")
            db.insert("book", null, cv)
        }
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 6) {
            db.execSQL("ALTER TABLE book ADD COLUMN pdf_url TEXT DEFAULT ''")
        } else {
            db.execSQL("DROP TABLE IF EXISTS account")
            db.execSQL("DROP TABLE IF EXISTS book")
            db.execSQL("DROP TABLE IF EXISTS favorite")
            onCreate(db)
        }
    }

    // Đăng ký tài khoản mới
    fun registerUser(username: String, password: String): Boolean {
        val db = writableDatabase
        val cv = ContentValues()
        cv.put("username", username)
        cv.put("password", password)
        cv.put("role", "user")

        val result = db.insert("account", null, cv)
        return result != -1L
    }

    fun checkUserExists(username: String): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM account WHERE username=?", arrayOf(username))
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

    fun checkLogin(username: String, password: String): String? {
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT role FROM account WHERE username=? AND password=?",
            arrayOf(username, password)
        )
        if (cursor.moveToFirst()) {
            val role = cursor.getString(0)
            cursor.close()
            return role
        }
        cursor.close()
        return null
    }

    // thêm sách
    fun addBook(title: String, author: String, image: String, description: String = "", pdfUrl: String = "") {
        val db = writableDatabase
        val cv = ContentValues()
        cv.put("title", title)
        cv.put("author", author)
        cv.put("image", image)
        cv.put("description", description)
        cv.put("pdf_url", pdfUrl)
        db.insert("book", null, cv)
    }

    // cập nhật sách
    fun updateBook(id: Int, title: String, author: String, image: String, description: String = "", pdfUrl: String = ""): Boolean {
        val db = writableDatabase
        val cv = ContentValues()
        cv.put("title", title)
        cv.put("author", author)
        cv.put("image", image)
        cv.put("description", description)
        cv.put("pdf_url", pdfUrl)
        val result = db.update("book", cv, "id=?", arrayOf(id.toString()))
        return result > 0
    }

    fun updateBookDescription(id: Int, description: String): Boolean {
        val db = writableDatabase
        val cv = ContentValues()
        cv.put("description", description)
        val result = db.update("book", cv, "id=?", arrayOf(id.toString()))
        return result > 0
    }

    fun deleteBook(id: Int): Boolean {
        val db = writableDatabase
        val result = db.delete("book", "id=?", arrayOf(id.toString()))
        db.delete("favorite", "book_id=?", arrayOf(id.toString()))
        return result > 0
    }

    fun getAllBooks(): ArrayList<Book> {
        val list = ArrayList<Book>()
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM book", null)
        while (cursor.moveToNext()) {
            val id = cursor.getInt(0)
            val title = cursor.getString(1)
            val author = cursor.getString(2)
            val image = cursor.getString(3)
            val description = if (cursor.columnCount > 4) cursor.getString(4) ?: "" else ""
            val pdfUrl = if (cursor.columnCount > 5) cursor.getString(5) ?: "" else ""
            list.add(Book(id, title, author, image, description, pdfUrl))
        }
        cursor.close()
        return list
    }

    fun getBookById(id: Int): Book? {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM book WHERE id=?", arrayOf(id.toString()))
        var book: Book? = null
        if (cursor.moveToFirst()) {
            val title = cursor.getString(1)
            val author = cursor.getString(2)
            val image = cursor.getString(3)
            val description = cursor.getString(4) ?: ""
            val pdfUrl = if (cursor.columnCount > 5) cursor.getString(5) ?: "" else ""
            book = Book(id, title, author, image, description, pdfUrl)
        }
        cursor.close()
        return book
    }

    fun addFavorite(username: String, bookId: Int) {
        val db = writableDatabase
        val cv = ContentValues()
        cv.put("username", username)
        cv.put("book_id", bookId)
        db.insertWithOnConflict("favorite", null, cv, SQLiteDatabase.CONFLICT_IGNORE)
    }

    fun removeFavorite(username: String, bookId: Int) {
        val db = writableDatabase
        db.delete("favorite", "username=? AND book_id=?", arrayOf(username, bookId.toString()))
    }

    fun isFavorite(username: String, bookId: Int): Boolean {
        val db = readableDatabase
        val cursor = db.rawQuery("SELECT * FROM favorite WHERE username=? AND book_id=?", arrayOf(username, bookId.toString()))
        val exists = cursor.count > 0
        cursor.close()
        return exists
    }

    fun getFavoriteBooks(username: String): ArrayList<Book> {
        val list = ArrayList<Book>()
        val db = readableDatabase
        val cursor = db.rawQuery(
            "SELECT book.* FROM book " +
                    "INNER JOIN favorite ON book.id = favorite.book_id " +
                    "WHERE favorite.username = ?",
            arrayOf(username)
        )
        while (cursor.moveToNext()) {
            val id = cursor.getInt(0)
            val title = cursor.getString(1)
            val author = cursor.getString(2)
            val image = cursor.getString(3)
            val description = if (cursor.columnCount > 4) cursor.getString(4) ?: "" else ""
            val pdfUrl = if (cursor.columnCount > 5) cursor.getString(5) ?: "" else ""
            list.add(Book(id, title, author, image, description, pdfUrl))
        }
        cursor.close()
        return list
    }
}
