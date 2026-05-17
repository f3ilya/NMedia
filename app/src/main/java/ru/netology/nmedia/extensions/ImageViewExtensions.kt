package ru.netology.nmedia.extensions

import android.widget.ImageView
import com.bumptech.glide.Glide
import ru.netology.nmedia.R

fun ImageView.load(url: String, circle: Boolean = false) {
    Glide.with(this)
        .load(url)
        .placeholder(R.drawable.ic_loading_48dp)
        .error(R.drawable.ic_error_48dp)
        .apply { if (circle) this.circleCrop() }
        .timeout(10_000)
        .into(this)
}