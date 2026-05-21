package ru.netology.nmedia.repository


import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import ru.netology.nmedia.api.PostsApi
import ru.netology.nmedia.dto.Post

class PostRepositoryRoomImpl : PostRepository {

    override fun getAllAsync(callback: PostRepository.PostCallback<List<Post>>) {
        PostsApi.retrofitService.getAll().enqueue(object : Callback<List<Post>> {
                override fun onResponse(call: Call<List<Post>>, response: Response<List<Post>>) {
                   if (!response.isSuccessful) {
                       when (response.code()) {
                           404 -> callback.onError(RuntimeException("Post not found"))
                           500 -> callback.onError(RuntimeException("Server error"))
                           else -> callback.onError(RuntimeException("Error: ${response.code()}"))
                       }
                       return
                   }
                    val body = response.body()
                    if (body == null) {
                        callback.onError(RuntimeException("Body is null"))
                    } else {
                        callback.onSuccess(body)
                    }
                }

                override fun onFailure(call: Call<List<Post>>, e: Throwable) {
                    callback.onError(e)
                }
            })
    }

    override fun likeByIdAsync(
        id: Long,
        likedByMe: Boolean,
        callback: PostRepository.PostCallback<Post>
    ) {
        val call = if (likedByMe) {
            PostsApi.retrofitService.dislikeById(id)
        } else {
            PostsApi.retrofitService.likeById(id)
        }
        call.enqueue(object : Callback<Post> {
                override fun onResponse(call: Call<Post>, response: Response<Post>) {
                    if (!response.isSuccessful) {
                        when (response.code()) {
                            404 -> callback.onError(RuntimeException("Post not found"))
                            500 -> callback.onError(RuntimeException("Server error"))
                            else -> callback.onError(RuntimeException("Error: ${response.code()}"))
                        }
                        return
                    }
                    val body = response.body()
                    if (body == null) {
                        callback.onError(RuntimeException("Body is null"))
                    } else {
                        callback.onSuccess(body)
                    }
                }

                override fun onFailure(call: Call<Post>, e: Throwable) {
                    callback.onError(e)
                }
            })
    }

    override fun shareById(id: Long) {
        TODO()
    }

    override fun saveAsync(post: Post, callback: PostRepository.PostCallback<Post>) {
        PostsApi.retrofitService.save(post)
            .enqueue(object : Callback<Post> {
                override fun onResponse(call: Call<Post>, response: Response<Post>) {
                    if (!response.isSuccessful) {
                        when (response.code()) {
                            404 -> callback.onError(RuntimeException("Post not found"))
                            500 -> callback.onError(RuntimeException("Server error"))
                            else -> callback.onError(RuntimeException("Error: ${response.code()}"))
                        }
                        return
                    }
                    val body = response.body()
                    if (body == null) {
                        callback.onError(RuntimeException("Body is null"))
                    } else {
                        callback.onSuccess(body)
                    }
                }

                override fun onFailure(call: Call<Post>, e: Throwable) {
                    callback.onError(e)
                }
            })
    }

    override fun removeByIdAsync(id: Long, callback: PostRepository.PostCallback<Unit>) {
        PostsApi.retrofitService.removeById(id)
            .enqueue(object : Callback<Unit> {
                override fun onResponse(call: Call<Unit>, response: Response<Unit>) {
                    if (!response.isSuccessful) {
                        when (response.code()) {
                            404 -> callback.onError(RuntimeException("Post not found"))
                            500 -> callback.onError(RuntimeException("Server error"))
                            else -> callback.onError(RuntimeException("Error: ${response.code()}"))
                        }
                        return
                    }
                    callback.onSuccess(Unit)
                }

                override fun onFailure(call: Call<Unit>, e: Throwable) {
                    callback.onError(e)
                }
            })
    }
}