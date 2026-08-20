package ru.netology.nmedia.dto

import ru.netology.nmedia.enumeration.AttachmentType

sealed class FeedItem{
    abstract val id: Long
}

data class Ad(
    override val id: Long,
    val url: String,
    val image: String,
) : FeedItem()

data class Post(
    override val id: Long,
    val authorId: Long,
    val author: String,
    val authorAvatar: String? = null,
    val content: String,
    val published: Long,
    val likes: Long,
    val likedByMe: Boolean,
    val share: Long,
    val views: Long,
    val isHiddenPost: Boolean = false,
    val attachment: Attachment? = null,
    val video: String? = null,
    val ownedByMe: Boolean = false,
) : FeedItem()

data class Attachment(
    val url: String,
    val description: String? = null,
    val type: AttachmentType,
)
