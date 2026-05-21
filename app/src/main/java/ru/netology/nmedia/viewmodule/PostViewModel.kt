package ru.netology.nmedia.viewmodule

import android.app.Application
import android.widget.Toast
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ru.netology.nmedia.R
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.model.FeedModel
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
        repository.getAllAsync(object : PostRepository.PostCallback<List<Post>> {
            override fun onSuccess(result: List<Post>) {
                _data.value = (FeedModel(posts = result, empty = result.isEmpty()))
            }

            override fun onError(e: Throwable) {
                _data.value = (FeedModel(error = true))
            }
        })
    }

    fun save() {
        edited.value?.let {
            repository.saveAsync(it, object : PostRepository.PostCallback<Post> {
                override fun onSuccess(result: Post) {
                    _postCreated.postValue(Unit)
                }

                override fun onError(e: Throwable) {
                    Toast.makeText(
                        getApplication(),
                        getApplication<Application>().getString(
                            R.string.error_loading
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                }
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
        repository.likeByIdAsync(id, likedByMe, object : PostRepository.PostCallback<Post> {
            override fun onSuccess(result: Post) {
                val posts = currentStatePosts.map {
                    if (it.id != id) it else result
                }
                _data.postValue(currentState.copy(posts = posts))
            }

            override fun onError(e: Throwable) {
                Toast.makeText(
                    getApplication(),
                    getApplication<Application>().getString(
                        R.string.error_loading
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            }
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
        repository.removeByIdAsync(id, object : PostRepository.PostCallback<Unit> {
            override fun onSuccess(result: Unit) {}

            override fun onError(e: Throwable) {
                _data.postValue(old)
                Toast.makeText(
                    getApplication(),
                    getApplication<Application>().getString(
                        R.string.error_loading
                    ),
                    Toast.LENGTH_SHORT
                ).show()
            }
        })
    }
}