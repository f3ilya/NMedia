package ru.netology.nmedia.activity

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.ActivityIntentHandlerBinding
import ru.netology.nmedia.databinding.ActivityNewPostBinding
import ru.netology.nmedia.util.AndroidUtils

class NewPostActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val binding = ActivityNewPostBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        val intent = getIntent()
        val editableText = intent.getStringExtra("content")
        with(binding) {
            edit.setText(editableText)
            AndroidUtils.showKeyboard(edit)
            ok.setOnClickListener {
                if (edit.text.isNullOrBlank()) {
                    setResult(Activity.RESULT_CANCELED)
                } else {
                    val content = edit.text.toString()
                    intent.putExtra(Intent.EXTRA_TEXT, content)
                    setResult(Activity.RESULT_OK, intent)
                }
                finish()
            }
            cancel.setOnClickListener {
                intent.putExtra(Intent.EXTRA_TEXT, editableText)
                setResult(Activity.RESULT_OK, intent)
                finish()
            }
        }
    }
}