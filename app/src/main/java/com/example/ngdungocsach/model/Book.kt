package com.example.ngdungocsach.model

import com.google.firebase.firestore.PropertyName

data class Book(
    val id: String = "", // Firebase thường dùng String làm ID (Push ID)
    val title: String = "",
    val author: String = "",
    val image: String = "",
    val description: String = "",
    val pdfUrl: String = "",
    val category: String = "Khác",
    @get:PropertyName("isHidden")
    @set:PropertyName("isHidden")
    var isHidden: Boolean = false,
    val viewCount: Int = 0,
    val favoriteCount: Int = 0
)
