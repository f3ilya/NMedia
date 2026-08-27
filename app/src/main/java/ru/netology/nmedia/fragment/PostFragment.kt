package ru.netology.nmedia.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.paging.filter
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.netology.nmedia.R
import ru.netology.nmedia.adapter.OnInteractionListener
import ru.netology.nmedia.adapter.FeedAdapter
import ru.netology.nmedia.databinding.FragmentPostBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.fragment.NewPostFragment.Companion.textArg
import ru.netology.nmedia.util.LongArg
import ru.netology.nmedia.viewmodule.PostViewModel

@AndroidEntryPoint
class PostFragment : Fragment() {

    companion object {
        var Bundle.idArg: Long? by LongArg
    }

    private val viewModel: PostViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentPostBinding.inflate(
            inflater,
            container,
            false
        )

        val adapter = FeedAdapter(object : OnInteractionListener {
            override fun onEdit(post: Post) {
                viewModel.edit(post)
                findNavController().navigate(
                    R.id.action_postFragment_to_newPostFragment,
                    Bundle().apply { textArg = post.content })
            }

            override fun onLike(post: Post) {
                viewModel.likeById(post.id, post.likedByMe)
            }

            override fun onRemove(post: Post) {
                viewModel.removeById(post.id)
                findNavController().navigateUp()
            }

            override fun onShare(post: Post) {
                viewModel.shareById(post.id)
                val intent = Intent().apply {
                    action = Intent.ACTION_SEND
                    putExtra(Intent.EXTRA_TEXT, post.content)
                    type = "text/plain"
                }
                val shareIntent = Intent.createChooser(
                    intent, getString(R.string.chooser_share_post)
                )
                startActivity(shareIntent)
            }

            override fun onVideo(post: Post) {
                val videoIntent = Intent.createChooser(
                    Intent(Intent.ACTION_VIEW, Uri.parse(post.video)),
                    getString(R.string.chooser_play_video)
                )
                startActivity(videoIntent)
            }

            override fun onPhoto(post: Post) {
                findNavController().navigate(
                    R.id.action_postFragment_to_photoFragment,
                    Bundle().apply { textArg = post.attachment?.url }
                )
            }
        })
        binding.list.adapter = adapter
        val postId = arguments?.idArg ?: -1
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.data.collectLatest {
                    adapter.submitData(it.filter { post -> post.id == postId })
                }
            }
        }
        return binding.root
    }
}