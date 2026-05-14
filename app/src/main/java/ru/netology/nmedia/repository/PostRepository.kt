package ru.netology.nmedia.repository

import ru.netology.nmedia.dto.Post

interface PostRepository {
    fun getAll(): List<Post>
    fun getAllAsync(callback: PostCallback)
    fun likeByIdAsync(id: Long, likedByMe: Boolean, callback: PostCallback)
    fun shareById(id: Long)
    fun saveAsync(post: Post, callback: PostCallback)
    fun removeByIdAsync(id: Long, callback: PostCallback)

    interface PostCallback {
        fun onSuccess(posts: List<Post>)
        fun onError(e: Exception)
    }
}