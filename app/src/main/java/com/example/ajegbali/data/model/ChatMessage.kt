package com.example.ajegbali.data.model

/**
 * Representasi satu pesan dalam percakapan chatbot.
 */
data class ChatMessage(
    val text: String,
    val isUser: Boolean,
    val timestamp: Long = System.currentTimeMillis()
)
