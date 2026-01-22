package ru.netology.nmedia.repository

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import ru.netology.nmedia.dto.Post

class PostRepositoryInMemoryImpl(private val context: Context) : PostRepository {
    private var posts = getPosts()
        set(value) {
            field = value
            sync()
        }

    private var nextId = (posts.maxOfOrNull { it.id } ?: 0) + 1

    private val data = MutableLiveData(posts)

    override fun getAll(): LiveData<List<Post>> = data

    override fun likeById(id: Long) {
        posts = posts.map {
            if (it.id != id) it else it.copy(
                likedByMe = !it.likedByMe, likes = if (it.likedByMe) it.likes - 1 else it.likes + 1
            )
        }
        data.value = posts
    }

    override fun shareById(id: Long) {
        posts = posts.map {
            if (it.id != id) it else it.copy(share = it.share + 1)
        }
        data.value = posts
    }

    override fun save(post: Post) {
        if (post.id == 0L) {
            posts = listOf(
                post.copy(
                    id = nextId++, author = "Me", likedByMe = false, published = "now"
                )
            ) + posts
            data.value = posts
            return
        }
        posts = posts.map {
            if (it.id != post.id) it else it.copy(content = post.content)
        }
        data.value = posts
    }

    override fun removeById(id: Long) {
        posts = posts.filter { it.id != id }
        data.value = posts
    }

    private fun sync() {
        context.openFileOutput(FILENAME, Context.MODE_PRIVATE).bufferedWriter().use {
            it.write(gson.toJson(posts))
        }
    }

    private fun getPosts(): List<Post> =
        context.filesDir.resolve(FILENAME)
            .takeIf { it.exists() }
            ?.inputStream()
            ?.bufferedReader()
            ?.use {
                gson.fromJson(it, type)
            } ?: emptyList()

    companion object {
        private const val FILENAME = "posts.json"
        private val gson = Gson()
        private val type = TypeToken.getParameterized(List::class.java, Post::class.java).type
    }
}