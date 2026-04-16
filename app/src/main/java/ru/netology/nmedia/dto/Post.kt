package ru.netology.nmedia.dto

data class Post(
    val id: Long,
    val author: String,
    val content: String,
    val published: Long,
    val likes: Long,
    val likedByMe: Boolean,
    val share: Long,
    val views: Long,
//    val video: String = ""
)
