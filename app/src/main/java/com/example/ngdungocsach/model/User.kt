package com.example.ngdungocsach.model

import com.google.firebase.firestore.PropertyName

data class User(
    var uid: String = "",
    var username: String = "",
    var role: String = "user",
    var subscriptionExpiry: Long = 0,
    
    @get:PropertyName("isBlocked")
    @set:PropertyName("isBlocked")
    var isBlocked: Boolean = false,
    
    @get:PropertyName("isHidden")
    @set:PropertyName("isHidden")
    var isHidden: Boolean = false
)
