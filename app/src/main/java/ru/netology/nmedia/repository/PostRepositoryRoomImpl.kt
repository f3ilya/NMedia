package ru.netology.nmedia.repository


import androidx.lifecycle.map
import okio.IOException
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.entity.toDto
import ru.netology.nmedia.api.PostsApi
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.entity.toEntity
import ru.netology.nmedia.error.ApiError
import ru.netology.nmedia.error.NetworkError
import ru.netology.nmedia.error.UnknownError

class PostRepositoryRoomImpl(private val dao: PostDao) : PostRepository {
    override val data = dao.getAll().map(List<PostEntity>::toDto)

    override suspend fun getAll() {
        try {
            val response = PostsApi.retrofitService.getAll()
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            val body = response.body() ?: throw ApiError(response.code(), response.message())
            dao.insert(body.toEntity())
        } catch (_: IOException) {
            throw NetworkError
        } catch (_: Exception) {
            throw UnknownError
        }
    }

    /** Сначала запрос на сервер, затем лайк/дизлайк: */

    /*    override suspend fun likeById(id: Long, likedByMe: Boolean) {
            try {
                val response = if (likedByMe) {
                    PostsApi.retrofitService.dislikeById(id)
                } else {
                    PostsApi.retrofitService.likeById(id)
                }
                if (!response.isSuccessful) {
                    throw ApiError(response.code(), response.message())
                }
                val body = response.body() ?: throw ApiError(response.code(), response.message())
                dao.insert(PostEntity.fromDto(body))
            } catch (_: IOException) {
                throw NetworkError
            } catch (_: Exception) {
                throw UnknownError
            }
        }*/

    override suspend fun likeById(id: Long, likedByMe: Boolean) {
        var isSuccess = false
        try {
            dao.likeById(id)
            val response = if (likedByMe) {
                PostsApi.retrofitService.dislikeById(id)
            } else {
                PostsApi.retrofitService.likeById(id)
            }
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            isSuccess = true
        } catch (_: IOException) {
            throw NetworkError
        } catch (_: Exception) {
            throw UnknownError
        } finally {
            if (!isSuccess) dao.likeById(id)
        }
    }

    override suspend fun shareById(id: Long) {
        dao.shareById(id)
    }

    override suspend fun save(post: Post) {
        try {
            val response = PostsApi.retrofitService.save(post)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            val body = response.body() ?: throw ApiError(response.code(), response.message())
            dao.insert(PostEntity.fromDto(body))
        } catch (_: IOException) {
            throw NetworkError
        } catch (_: Exception) {
            throw UnknownError
        }
    }

    /** Сначала запрос на сервер, затем удаление: */

    /*
        override suspend fun removeById(id: Long) {
            try {
                val response = PostsApi.retrofitService.removeById(id)
                if (!response.isSuccessful) {
                    throw ApiError(response.code(), response.message())
                }
                dao.removeById(id)
            } catch (_: IOException) {
                throw NetworkError
            } catch (_: Exception) {
                throw UnknownError
            }
        }
    */

    override suspend fun removeById(id: Long) {
        val posts = data.value ?: return
        val post = posts.find { it.id == id } ?: return
        var isSuccess = false
        try {
            dao.removeById(id)
            val response = PostsApi.retrofitService.removeById(id)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            isSuccess = true
        } catch (_: IOException) {
            throw NetworkError
        } catch (_: Exception) {
            throw UnknownError
        } finally {
            if (!isSuccess) dao.insert(PostEntity.fromDto(post))
        }
    }
}