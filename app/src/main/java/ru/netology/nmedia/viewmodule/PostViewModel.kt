package ru.netology.nmedia.viewmodule

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.dto.MediaUpload
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.model.FeedModelState
import ru.netology.nmedia.model.PhotoModel
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.repository.PostRepositoryRoomImpl
import ru.netology.nmedia.util.SingleLiveEvent
import java.io.File

private val empty = Post(
    id = 0,
    author = "",
    authorAvatar = "",
    content = "",
    published = 0,
    likes = 0,
    likedByMe = false,
    share = 0,
    views = 0,
    isHiddenPost = false
)

private val noPhoto = PhotoModel()
class PostViewModel(application: Application) : AndroidViewModel(application) {
    // упрощённый вариант
    private val repository: PostRepository =
        PostRepositoryRoomImpl(AppDb.getInstance(application).postDao)
    val data: LiveData<FeedModel> = repository.data
        .map(::FeedModel)
        .asLiveData(Dispatchers.Default)
    private val _dataState = MutableLiveData<FeedModelState>()
    val dataState: LiveData<FeedModelState>
        get() = _dataState
    val newerCount: LiveData<Int> = data.switchMap {
        repository.getNewerCount(it.posts.firstOrNull()?.id ?: 0L)
            .catch { e -> e.printStackTrace() }
            .asLiveData(Dispatchers.Default)
    }
    val edited = MutableLiveData(empty)
    private val _postCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit>
        get() = _postCreated

    private val _photo = MutableLiveData(noPhoto)
    val photo: LiveData<PhotoModel>
        get() = _photo

    init {
        loadPosts()
    }

    fun loadPosts(refresh: Boolean = false) = viewModelScope.launch {
        runCatching {
            _dataState.value =
                if (refresh) FeedModelState(refreshing = true) else FeedModelState(loading = true)
            repository.getAll()
            _dataState.value = FeedModelState()
        }.onFailure {
            _dataState.value = FeedModelState(error = true)
        }
    }

    fun save() {
        edited.value?.let {
            _postCreated.value = Unit
            viewModelScope.launch {
                runCatching {
                    when(_photo.value) {
                        noPhoto -> repository.save(it)
                        else -> _photo.value?.file?.let { file ->
                            repository.saveWithAttachment(it, MediaUpload(file))
                        }
                    }
                    _dataState.value = FeedModelState()
                }.onFailure {
                    _dataState.value = FeedModelState(error = true)
                }
            }
        }
        clearEdited()
    }

    fun edit(post: Post) {
        edited.value = post
    }

    fun changeContent(content: String) {
        val text = content.trim()
        if (edited.value?.content == text) {
            return
        }
        edited.value = edited.value?.copy(content = text)
    }

    fun changePhoto(uri: Uri?, file: File?) {
        _photo.value = PhotoModel(uri, file)
    }

    fun clearEdited() {
        edited.value = empty
        _photo.value = noPhoto
    }

    fun likeById(id: Long) {
        val currentState = data.value ?: return
        val currentStatePosts = currentState.posts
        val post = currentStatePosts.find { it.id == id } ?: return
        val likedByMe = post.likedByMe
        viewModelScope.launch {
            runCatching {
                repository.likeById(id, likedByMe)
                _dataState.value = FeedModelState()
            }.onFailure {
                _dataState.value = FeedModelState(error = true)
            }
        }
    }

    fun shareById(id: Long) {
        viewModelScope.launch {
            repository.shareById(id)
        }
    }

    fun removeById(id: Long) {
        viewModelScope.launch {
            runCatching {
                repository.removeById(id)
                _dataState.value = FeedModelState()
            }.onFailure {
                _dataState.value = FeedModelState(error = true)
            }
        }
    }

    fun readAll() {
        viewModelScope.launch {
            runCatching {
                repository.readAll()
                _dataState.value = FeedModelState()
            }.onFailure {
                _dataState.value = FeedModelState(error = true)
            }
        }
    }
}