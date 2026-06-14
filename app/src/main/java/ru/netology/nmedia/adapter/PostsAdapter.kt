package ru.netology.nmedia.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.netology.nmedia.BuildConfig
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.CardPostBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.extensions.load
import ru.netology.nmedia.util.counter
import ru.netology.nmedia.enumeration.AttachmentType.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

interface OnInteractionListener {
    fun onLike(post: Post) {}
    fun onEdit(post: Post) {}
    fun onRemove(post: Post) {}
    fun onShare(post: Post) {}
    fun onVideo(post: Post) {}
    fun onPost(post: Post) {}
    fun onPhoto(post: Post) {}
}

class PostsAdapter(
    private val onInteractionListener: OnInteractionListener
) : ListAdapter<Post, PostViewHolder>(PostDiffCallback) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PostViewHolder {
        val binding = CardPostBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return PostViewHolder(binding, onInteractionListener)
    }

    override fun onBindViewHolder(holder: PostViewHolder, position: Int) {
        val post = getItem(position)
        holder.bind(post)
    }
}

class PostViewHolder(
    private val binding: CardPostBinding,
    private val onInteractionListener: OnInteractionListener
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(post: Post) {
        binding.apply {
            binding.avatar.load("${BuildConfig.BASE_URL}/avatars/${post.authorAvatar}", true)
            author.text = post.author
            content.text = post.content
            published.text = Instant.ofEpochSecond(post.published)
                .atZone(ZoneId.systemDefault())
                .format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss"))
            like.text = counter(post.likes)
            share.text = counter(post.share)
            views.text = counter(post.views)
            like.isChecked = post.likedByMe
            when (post.attachment?.type) {
                VIDEO -> {
                    group.visibility = View.GONE
                    video.setOnClickListener {
                        onInteractionListener.onVideo(post)
                    }
                }

                IMAGE -> {
                    binding.attachment.load("${BuildConfig.BASE_URL}/media/${post.attachment.url}")
                    attachment.visibility = View.VISIBLE
                    attachment.contentDescription = post.attachment.description
                    attachment.setOnClickListener {
                        onInteractionListener.onPhoto(post)
                    }
                }

                else -> {
                    group.visibility = View.GONE
                    attachment.visibility = View.GONE
                }
            }
            like.setOnClickListener {
                onInteractionListener.onLike(post)
                like.isChecked = post.likedByMe
            }
            share.setOnClickListener {
                onInteractionListener.onShare(post)
            }
            play.setOnClickListener {
                onInteractionListener.onVideo(post)
            }
            content.setOnClickListener {
                onInteractionListener.onPost(post)
            }
            menu.setOnClickListener {
                PopupMenu(it.context, it).apply {
                    inflate(R.menu.options_post)
                    setOnMenuItemClickListener { item ->
                        when (item.itemId) {
                            R.id.remove -> {
                                onInteractionListener.onRemove(post)
                                true
                            }

                            R.id.edit -> {
                                onInteractionListener.onEdit(post)
                                true
                            }

                            else -> false
                        }
                    }
                }.show()
            }
        }
    }
}

object PostDiffCallback : DiffUtil.ItemCallback<Post>() {
    override fun areItemsTheSame(oldItem: Post, newItem: Post) = oldItem.id == newItem.id

    override fun areContentsTheSame(oldItem: Post, newItem: Post) = oldItem == newItem
}

