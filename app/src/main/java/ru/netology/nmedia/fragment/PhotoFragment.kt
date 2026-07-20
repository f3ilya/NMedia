package ru.netology.nmedia.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nmedia.BuildConfig
import ru.netology.nmedia.databinding.FragmentPhotoBinding
import ru.netology.nmedia.extensions.load
import ru.netology.nmedia.util.StringArg

@AndroidEntryPoint
class PhotoFragment : Fragment() {

    companion object {
        var Bundle.textArg: String? by StringArg
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentPhotoBinding.inflate(
            inflater,
            container,
            false
        )

        binding.attachment.load("${BuildConfig.BASE_URL}/media/${arguments?.textArg}")

        return binding.root
    }
}