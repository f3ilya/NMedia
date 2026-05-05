package ru.netology.nmedia.viewmodule

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import okio.IOException
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.repository.PostRepositoryRoomImpl
import ru.netology.nmedia.util.SingleLiveEvent
import kotlin.concurrent.thread

private val empty = Post(
    id = 0,
    author = "",
    content = "",
    published = 0,
    likes = 0,
    likedByMe = false,
    share = 0,
    views = 0
)

class PostViewModel(application: Application) : AndroidViewModel(application) {
    // упрощённый вариант
    private val repository: PostRepository = PostRepositoryRoomImpl()
    private val _data = MutableLiveData(FeedModel())
    val data: LiveData<FeedModel>
        get() = _data
    val edited = MutableLiveData(empty)
    private val _postCreated = SingleLiveEvent<Unit>()
    val postCreated: LiveData<Unit>
        get() = _postCreated

    init {
        loadPosts()
    }

    fun loadPosts() {
        thread {
            _data.postValue(FeedModel(loading = true))
            try {
                val posts = repository.getAll()
                FeedModel(posts = posts, empty = posts.isEmpty())
            } catch (_: IOException) {
                FeedModel(error = true)
            }.also(_data::postValue)
        }
    }

    fun save() {
        edited.value?.let {
            thread {
                repository.save(it)
                _postCreated.postValue(Unit)
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
        thread {
            val post: Post
            if (_data.value?.posts?.find { it.id == id }?.likedByMe == false) {
                post = repository.likeById(id)
            } else {
                post = repository.dislikeById(id)
            }
            val posts = _data.value?.posts?.map {
                if (it.id != id) it else it.copy(likes = post.likes, likedByMe = post.likedByMe)
            }
            _data.postValue(_data.value?.copy(posts = posts.orEmpty()))
        }
    }
    fun shareById(id: Long) = thread {
        val posts = _data.value?.posts?.map {
            if (it.id != id) it else it.copy(share = it.share + 1)
        }
        _data.postValue(_data.value?.copy(posts = posts.orEmpty()))
    }
    fun removeById(id: Long) {
        thread {
        val old = _data.value?.posts.orEmpty()
        _data.postValue(
            _data.value?.copy(
                posts = _data.value?.posts.orEmpty()
                    .filter { it.id != id }
            )
        )
        try {
            repository.removeById(id)
        } catch (_: IOException) {
            _data.postValue(_data.value?.copy(posts = old))
        }
            }
    }
}