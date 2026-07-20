package ru.netology.nmedia.fragment

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.navigation.fragment.findNavController
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import ru.netology.nmedia.R
import ru.netology.nmedia.adapter.OnInteractionListener
import ru.netology.nmedia.adapter.PostsAdapter
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.databinding.FragmentFeedBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.fragment.NewPostFragment.Companion.textArg
import ru.netology.nmedia.fragment.PostFragment.Companion.idArg
import ru.netology.nmedia.util.showConfirmationDialog
import ru.netology.nmedia.viewmodule.PostViewModel
import javax.inject.Inject
import kotlin.coroutines.EmptyCoroutineContext

@AndroidEntryPoint
class FeedFragment : Fragment() {
    @Inject
    lateinit var auth: AppAuth
    private val viewModel: PostViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val binding = FragmentFeedBinding.inflate(
            inflater, container, false
        )

        fun authorizationIsRequired() {
            requireContext().showConfirmationDialog(
                title = getString(R.string.authorization_is_required),
                message = getString(R.string.authorization_is_required_message),
                onConfirm = fun() {
                    findNavController().navigate(R.id.action_feedFragment_to_authenticationFragment)
                },
                positive = getString(R.string.log_in)
            )
        }

        val adapter = PostsAdapter(object : OnInteractionListener {
            override fun onEdit(post: Post) {
                viewModel.edit(post)
                findNavController().navigate(
                    R.id.action_feedFragment_to_newPostFragment,
                    Bundle().apply { textArg = post.content })
            }

            override fun onLike(post: Post) {
                if (auth.authStateFlow.value.token.isNullOrEmpty()) {
                    authorizationIsRequired()
                } else {
                    viewModel.likeById(post.id)
                }
            }

            override fun onRemove(post: Post) {
                viewModel.removeById(post.id)
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

            override fun onPost(post: Post) {
                findNavController().navigate(
                    R.id.action_feedFragment_to_postFragment,
                    Bundle().apply { idArg = post.id }
                )
            }

            override fun onPhoto(post: Post) {
                findNavController().navigate(
                    R.id.action_feedFragment_to_photoFragment,
                    Bundle().apply { textArg = post.attachment?.url }
                )
            }
        })
        binding.list.adapter = adapter
        viewModel.dataState.observe(viewLifecycleOwner) { state ->
            binding.progress.isVisible = state.loading
            binding.swipeRefresh.isRefreshing = state.refreshing
            if (state.error) {
                Snackbar.make(binding.root, R.string.error_loading, Snackbar.LENGTH_LONG)
                    .setAction(R.string.retry_loading) { viewModel.loadPosts() }
                    .show()
            }
        }

        viewModel.data.observe(viewLifecycleOwner) { state ->
            adapter.submitList(state.posts)
            binding.emptyText.isVisible = state.empty
        }

        viewModel.newerCount.observe(viewLifecycleOwner) { state ->
            binding.newPosts.text = getString(R.string.recent_posts)
            binding.newPosts.isVisible = state > 0
        }

        binding.newPosts.setOnClickListener {
            binding.newPosts.isVisible = false
            viewModel.readAll()
            CoroutineScope(EmptyCoroutineContext).launch {
                delay(100)
                binding.list.post {
                    binding.list.smoothScrollToPosition(0)
                }
            }
        }

        binding.swipeRefresh.setOnRefreshListener {
            viewModel.loadPosts(refresh = true)
        }

        binding.fab.setOnClickListener {
            if (auth.authStateFlow.value.token.isNullOrEmpty()) {
                authorizationIsRequired()
            } else {
                findNavController().navigate(R.id.action_feedFragment_to_newPostFragment)
            }
        }

        return binding.root
    }
}