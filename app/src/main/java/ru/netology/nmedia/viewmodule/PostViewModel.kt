package ru.netology.nmedia.viewmodule

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.FeedModel
import ru.netology.nmedia.repository.PostRepository
import ru.netology.nmedia.repository.PostRepositoryRoomImpl
import ru.netology.nmedia.util.SingleLiveEvent

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
        _data.value = FeedModel(loading = true)
        repository.getAllAsync(object : PostRepository.PostCallback {
            override fun onSuccess(posts: List<Post>) {
                _data.postValue(FeedModel(posts = posts, empty = posts.isEmpty()))
            }

            override fun onError(e: Exception) {
                _data.postValue(FeedModel(error = true))
            }
        })
    }

    fun save() {
        edited.value?.let {
            repository.saveAsync(it, object : PostRepository.PostCallback {
                override fun onSuccess(posts: List<Post>) {
                    _postCreated.postValue(Unit)
                }

                override fun onError(e: Exception) {}
            })
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
        val currentState = _data.value ?: return
        val currentStatePosts = currentState.posts
        val post = currentStatePosts.find { it.id == id } ?: return
        val likedByMe = post.likedByMe
        repository.likeByIdAsync(id, likedByMe, object : PostRepository.PostCallback {
            override fun onSuccess(posts: List<Post>) {
                val post = currentStatePosts.map { post ->
                    if (post.id != id) post else posts.find { it.id == id } ?: return
                }
                _data.postValue(currentState.copy(posts = post))
            }

            override fun onError(e: Exception) {}
        })
    }

    fun shareById(id: Long) {
        val posts = _data.value?.posts?.map {
            if (it.id != id) it else it.copy(share = it.share + 1)
        }
        _data.postValue(_data.value?.copy(posts = posts.orEmpty()))
    }

    fun removeById(id: Long) {
        val old = _data.value ?: return
        _data.postValue(old.copy(posts = old.posts.filter { it.id != id }))
        repository.removeByIdAsync(id, object : PostRepository.PostCallback {
            override fun onSuccess(posts: List<Post>) {}

            override fun onError(e: Exception) {
                _data.postValue(old)
            }
        })
    }
}