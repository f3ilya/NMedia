package ru.netology.nmedia.repository

import androidx.paging.ExperimentalPagingApi
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.map
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okio.IOException
import ru.netology.nmedia.api.ApiService
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.dao.PostDao
import ru.netology.nmedia.dao.PostRemoteKeyDao
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.dto.Attachment
import ru.netology.nmedia.dto.Media
import ru.netology.nmedia.dto.MediaUpload
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.entity.PostEntity
import ru.netology.nmedia.entity.toEntity
import ru.netology.nmedia.enumeration.AttachmentType
import ru.netology.nmedia.error.ApiError
import ru.netology.nmedia.error.AppError
import ru.netology.nmedia.error.NetworkError
import ru.netology.nmedia.error.UnknownError
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.milliseconds

@Singleton
class PostRepositoryImpl @Inject constructor(
    appDb: AppDb,
    postRemoteKeyDao: PostRemoteKeyDao,
    private val dao: PostDao,
    private val api: ApiService,
    private val auth: AppAuth,
) : PostRepository {
    @OptIn(ExperimentalPagingApi::class)
    override val data: Flow<PagingData<Post>> = Pager(
        config = PagingConfig(pageSize = 25, enablePlaceholders = false),
        remoteMediator = PostRemoteMediator(api, appDb, dao, postRemoteKeyDao),
        pagingSourceFactory = dao::pagingSource,
    ).flow.map { pagingData ->
        pagingData.map(PostEntity::toDto)
    }

    /** Было до 3.2: */
    /*
    override val data = dao.getAll()
        .map(List<PostEntity>::toDto)
        .flowOn(Dispatchers.Default)
     */

    override suspend fun getAll() {
        try {
            val response = api.getAll()
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

    override fun getNewerCount(id: Long): Flow<Int> = flow {
        while (true) {
            delay(10_000L.milliseconds)
            val response = api.getNewer(id)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            val body = response.body() ?: throw ApiError(response.code(), response.message())
            dao.insert(body.toEntity().map {
                if (it.authorId != auth.authStateFlow.value.id) {
                    it.copy(isHiddenPost = true)
                } else {
                    it.copy()
                }
            })
            emit(body.size)
        }
    }
        .catch { e -> throw AppError.from(e) }
        .flowOn(Dispatchers.Default)

    /** Сначала запрос на сервер, затем лайк/дизлайк: */

    /*    override suspend fun likeById(id: Long, likedByMe: Boolean) {
            try {
                val response = if (likedByMe) {
                    api.dislikeById(id)
                } else {
                    api.likeById(id)
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
                api.dislikeById(id)
            } else {
                api.likeById(id)
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
            val response = api.save(post)
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

    override suspend fun saveWithAttachment(post: Post, upload: MediaUpload) {
        try {
            val media = upload(upload)
            val postWithAttachment = post.copy(attachment = Attachment(media.id, type = AttachmentType.IMAGE))
            save(postWithAttachment)
        } catch (e: AppError) {
            throw e
        } catch (_: IOException) {
            throw NetworkError
        } catch (_: Exception) {
            throw UnknownError
        }
    }

    /** Сначала запрос на сервер, затем удаление: */

        override suspend fun removeById(id: Long) {
            try {
                val response = api.removeById(id)
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

    /** Сначала удаление, затем запрос на сервер: */
    /*
    override suspend fun removeById(id: Long) {
        val postFlow = data.map { state ->
            state.find { it.id == id }
        }
        val post = postFlow.first() ?: return
        var isSuccess = false
        try {
            dao.removeById(id)
            val response = api.removeById(id)
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
     */

    override suspend fun readAll() {
        dao.readAll()
    }

    override suspend fun upload(upload: MediaUpload): Media {
        try {
            val media = MultipartBody.Part.createFormData(
                "file", upload.file.name, upload.file.asRequestBody()
            )
            val response = api.upload(media)
            if (!response.isSuccessful) {
                throw ApiError(response.code(), response.message())
            }
            return response.body() ?: throw ApiError(response.code(), response.message())
        } catch (_: IOException) {
            throw NetworkError
        } catch (_: Exception) {
            throw UnknownError
        }
    }
}