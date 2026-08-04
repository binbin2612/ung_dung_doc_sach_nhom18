package com.example.ngdungocsach.model

data class Payment(
    var id: String = "",
    val username: String = "",
    val packageName: String = "",
    val amount: Long = 0,
    val timestamp: Long = 0,
    val status: String = "completed" // In this demo we assume immediate success after user confirms
)
