package ru.netology.nmedia.entity

import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.PrimaryKey
import ru.netology.nmedia.dto.Attachment
import ru.netology.nmedia.dto.Post

@Entity
data class PostEntity(
    @PrimaryKey(autoGenerate = true) val id: Long,
    val author: String,
    val authorAvatar: String?,
    val content: String,
    val published: Long,
    val likes: Long = 0,
    val likedByMe: Boolean,
    val share: Long,
    val views: Long,
    @Embedded
    val attachment: Attachment? = null,
    val video: String? = null
) {
    fun toDto() = Post(id, author, authorAvatar, content, published, likes, likedByMe, share, views, attachment)

    companion object {
        fun fromDto(dto: Post) = PostEntity(
            dto.id,
            dto.author,
            dto.authorAvatar,
            dto.content,
            dto.published,
            dto.likes,
            dto.likedByMe,
            dto.share,
            dto.views,
            dto.attachment,
        )
    }
}

fun List<PostEntity>.toDto(): List<Post> = map(PostEntity::toDto)
fun List<Post>.toEntity(): List<PostEntity> = map(PostEntity::fromDto)