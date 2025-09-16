package com.example.simplepad

import java.util.UUID

data class Note(
    val id: String = newId(),
    val title: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long
) {
    companion object {
        fun newId(): String = UUID.randomUUID().toString()
    }
}
