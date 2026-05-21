package ru.netology.nmedia.repository

import ru.netology.nmedia.dto.Post

interface PostRepository {
    fun getAllAsync(callback: PostCallback<List<Post>>)
    fun likeByIdAsync(id: Long, likedByMe: Boolean, callback: PostCallback<Post>)
    fun shareById(id: Long)
    fun saveAsync(post: Post, callback: PostCallback<Post>)
    fun removeByIdAsync(id: Long, callback: PostCallback<Unit>)

    interface PostCallback<T> {
        fun onSuccess(result: T) {}
        fun onError(e: Throwable) {}
    }
}