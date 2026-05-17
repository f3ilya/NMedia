package ru.netology.nmedia.dto

data class Post(
    val id: Long,
    val author: String,
    val authorAvatar: String? = null,
    val content: String,
    val published: Long,
    val likes: Long,
    val likedByMe: Boolean,
    val share: Long,
    val views: Long,
    val attachment: Attachment? = null,
//    val video: String = ""
)

data class Attachment(
    val url: String,
    val description: String?,
    val type: AttachmentType,
)

enum class AttachmentType {
    IMAGE
}
