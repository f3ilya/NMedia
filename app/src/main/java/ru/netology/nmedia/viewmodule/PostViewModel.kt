package ru.netology.nmedia.viewmodule

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import ru.netology.nmedia.db.AppDb
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.model.FeedModelState
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.repository.PostRepositoryRoomImpl
import ru.netology.nmedia.util.SingleLiveEvent

private val empty = Post(
    id = 0,
    author = "",
    authorAvatar = "",
    content = "",
    published = 0,
    likes = 0,
    likedByMe = false,
    share = 0,
    views = 0
)

class PostViewModel(application: Application) : AndroidViewModel(application) {
    // упрощённый вариант
    private val repository: PostRepository =
        PostRepositoryRoomImpl(AppDb.getInstance(application).postDao)
    val data: LiveData<FeedModel> = repository.data.map(::FeedModel)
    private val _dataState = MutableLiveData<FeedModelState>()
    val dataState: LiveData<FeedModelState>
        get() = _dataState
    val edited = MutableLiveData(empty)
    private val _postCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit>
        get() = _postCreated

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
                    repository.save(it)
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

    fun clearEdited() {
        edited.value = empty
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
}