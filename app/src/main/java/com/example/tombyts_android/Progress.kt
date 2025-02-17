package com.example.tombyts_android

data class ProgressResponse(
    val userId: String,
    val movieId: String,
    val progress: Double,
    val updatedAt: String
)

data class ProgressUpdate(
    val progress: Int
)