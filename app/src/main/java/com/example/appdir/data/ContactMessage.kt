package com.example.appdir.data

data class ContactMessage(
    val name: String,
    val email: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis()
)
