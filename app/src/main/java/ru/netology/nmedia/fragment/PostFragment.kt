package ru.netology.nmedia.fragment

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import ru.netology.nmedia.R
import ru.netology.nmedia.adapter.OnInteractionListener
import ru.netology.nmedia.adapter.PostsAdapter
import ru.netology.nmedia.databinding.FragmentPostBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.fragment.NewPostFragment.Companion.textArg
import ru.netology.nmedia.util.LongArg
import ru.netology.nmedia.viewmodule.PostViewModel

class PostFragment : Fragment() {

    companion object {
        var Bundle.idArg: Long? by LongArg
    }

    private val viewModel: PostViewModel by viewModels(ownerProducer = ::requireParentFragment)

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

        val adapter = PostsAdapter(object : OnInteractionListener {
            override fun onEdit(post: Post) {
                viewModel.edit(post)
                findNavController().navigate(
                    R.id.action_postFragment_to_newPostFragment,
                    Bundle().apply { textArg = post.content })
            }

            override fun onLike(post: Post) {
                viewModel.likeById(post.id)
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

//            override fun onVideo(post: Post) {
//                val videoIntent = Intent.createChooser(
//                    Intent(Intent.ACTION_VIEW, post.video.toUri()),
//                    getString(R.string.chooser_play_video)
//                )
//                startActivity(videoIntent)
//            }
        })
        binding.list.adapter = adapter
        val postId = arguments?.idArg ?: -1
        viewModel.data.observe(viewLifecycleOwner) { state ->
            val post = state.posts.find { it.id == postId } ?: return@observe
            adapter.submitList(listOf(post))
        }
        return binding.root
    }
}