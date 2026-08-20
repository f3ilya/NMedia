package ru.netology.nmedia.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.widget.PopupMenu
import androidx.core.view.isVisible
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.viewbinding.ViewBinding
import ru.netology.nmedia.BuildConfig
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.CardAdBinding
import ru.netology.nmedia.databinding.CardPostBinding
import ru.netology.nmedia.dto.Ad
import ru.netology.nmedia.dto.FeedItem
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.enumeration.AttachmentType.IMAGE
import ru.netology.nmedia.enumeration.AttachmentType.VIDEO
import ru.netology.nmedia.extensions.load
import ru.netology.nmedia.util.counter
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
    fun onAdClick(ad: Ad) {}
}

class FeedAdapter(
    private val onInteractionListener: OnInteractionListener
) : PagingDataAdapter<FeedItem, RecyclerView.ViewHolder>(FeedItemDiffCallback) {
    private val typeAd = 0
    private val typePost = 1

    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is Ad -> typeAd
            is Post -> typePost
//            null -> error("unknown item type")
            null -> typePost
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val layoutInflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            typeAd -> AdViewHolder(
                CardAdBinding.inflate(layoutInflater, parent, false),
                onInteractionListener
            )

            typePost -> PostViewHolder(
                CardPostBinding.inflate(layoutInflater, parent, false),
                onInteractionListener
            )

            else -> error("unknown view type: $viewType")
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = getItem(position) ?: return
        when (item) {
            is Ad -> (holder as? AdViewHolder)?.bind(item)
            is Post -> (holder as? PostViewHolder)?.bind(item)
        }
        /** Было до 3.2: */
        /*
        val post = getItem(position)
        holder.bind(post)
         */
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
            menu.isVisible = post.ownedByMe
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

class AdViewHolder(
    private val binding: CardAdBinding,
    private val onInteractionListener: OnInteractionListener,
) : RecyclerView.ViewHolder(binding.root) {
    fun bind(ad: Ad) {
        binding.apply {
            image.load("${BuildConfig.BASE_URL}/media/${ad.image}")
            image.setOnClickListener {
                onInteractionListener.onAdClick(ad)
            }
        }
    }
}

object FeedItemDiffCallback : DiffUtil.ItemCallback<FeedItem>() {
    override fun areItemsTheSame(oldItem: FeedItem, newItem: FeedItem): Boolean {
        if (oldItem::class != newItem::class) {
            return false
        }
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: FeedItem, newItem: FeedItem): Boolean {
        return oldItem == newItem
    }
}

