package ru.netology.nmedia.dto

data class Post(
    val id: Long,
    val author: String,
    val content: String,
    val published: String,
    var likes: Long,
    var likedByMe: Boolean,
    var share: Long,
    var views: Long
)

fun counter(count: Long): String {
    val letter = when {
        count < 1_000_000 -> "K"
        count in 1_000_000..999_999_999 -> "M"
        else -> "B"
    }
    val amount = when {
        count < 1_000_000 -> count
        count in 1_000_000..999_999_999 -> count / 1_000
        else -> count / 1_000_000
    }

    return when {
        count < 1_000 -> count.toString()
        count in 10_000..999_999 -> (count / 1_000).toString().plus(letter)
        else -> if (amount / 100 % 10 == 0L) {
            (amount / 1_000).toString().plus(letter)
        } else {
            (amount / 1_000).toString()
                .plus(".")
                .plus((amount / 100 % 10))
                .plus(letter)
        }
    }
}
