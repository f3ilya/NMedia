package ru.netology.nmedia.repository

import ru.netology.nmedia.dto.Post

interface PostRepository {
    fun getAll(): List<Post>
    fun getAllAsync(callback: GetCallback)
    fun likeById(id: Long): Post
    fun likeByIdAsync(id: Long, callback: GetCallback)
    fun dislikeById(id: Long): Post
    fun dislikeByIdAsync(id: Long, callback: GetCallback)
    fun shareById(id: Long)
    fun save(post: Post): Post
    fun saveAsync(post: Post, callback: GetCallback)
    fun removeById(id: Long)
    fun removeByIdAsync(id: Long, callback: GetCallback)

    interface GetCallback {
        fun onSuccess(posts: List<Post>)
        fun onError(e: Exception)
    }
}