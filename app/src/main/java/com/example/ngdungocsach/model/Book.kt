package com.example.ngdungocsach.model

data class Book(
    val id: Int,
    val title: String,
    val author: String,
    val image: String,
    val description: String = "",
    val pdfUrl: String = ""
)
