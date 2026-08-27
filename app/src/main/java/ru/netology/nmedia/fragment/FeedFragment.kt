package ru.netology.nmedia.fragment

import android.content.Intent
import android.graphics.Canvas
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.netology.nmedia.R
import ru.netology.nmedia.adapter.OnInteractionListener
import ru.netology.nmedia.adapter.FeedAdapter
import ru.netology.nmedia.adapter.PagingLoadStateAdapter
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.databinding.FragmentFeedBinding
import ru.netology.nmedia.dto.Post
import ru.netology.nmedia.fragment.NewPostFragment.Companion.textArg
import ru.netology.nmedia.fragment.PostFragment.Companion.idArg
import ru.netology.nmedia.util.showConfirmationDialog
import ru.netology.nmedia.viewmodule.PostViewModel
import javax.inject.Inject
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.time.Duration.Companion.milliseconds
import android.graphics.Color
import android.util.TypedValue
import androidx.core.content.ContextCompat
import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator

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

        val adapter = FeedAdapter(object : OnInteractionListener {
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
                    viewModel.likeById(post.id, post.likedByMe)
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
        binding.list.adapter = adapter.withLoadStateHeaderAndFooter(
            header = PagingLoadStateAdapter { adapter.retry() },
            footer = PagingLoadStateAdapter { adapter.retry() },
        )

        val itemTouchHelper: ItemTouchHelper? = null
        ItemTouchHelper(object : ItemTouchHelper.SimpleCallback(
            0, ItemTouchHelper.START or ItemTouchHelper.END
        ) {
            override fun onMove(
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                target: RecyclerView.ViewHolder
            ): Boolean = false

            override fun onSwiped(
                viewHolder: RecyclerView.ViewHolder,
                direction: Int
            ) {
                val position = viewHolder.bindingAdapterPosition
                val feedItem = adapter.getItemAt(position)
                val post = feedItem as? Post

                if (post == null || !post.ownedByMe) {
                    adapter.notifyItemChanged(position)
                    return
                }

                when (direction) {
                    ItemTouchHelper.START -> {
                        requireContext().showConfirmationDialog(
                            title = getString(R.string.do_you_really_want_to_delete_the_post),
                            message = getString(R.string.this_action_cannot_be_undone),
                            positive = getString(R.string.ok),
                            onConfirm = {
                                viewModel.removeById(post.id)
                            },
                            onCancel = {
                                itemTouchHelper?.attachToRecyclerView(null)
                                adapter.notifyItemChanged(position)
                                itemTouchHelper?.attachToRecyclerView(binding.list)
                            }
                        )
                    }

                    ItemTouchHelper.END -> {
                        viewModel.edit(post)
                        findNavController().navigate(
                            R.id.action_feedFragment_to_newPostFragment,
                            Bundle().apply { textArg = post.content }
                        )
                        itemTouchHelper?.attachToRecyclerView(null)
                        adapter.notifyItemChanged(position)
                        itemTouchHelper?.attachToRecyclerView(binding.list)
                    }
                }
            }

            override fun onChildDraw(
                c: Canvas,
                recyclerView: RecyclerView,
                viewHolder: RecyclerView.ViewHolder,
                dX: Float,
                dY: Float,
                actionState: Int,
                isCurrentlyActive: Boolean
            ) {
                RecyclerViewSwipeDecorator.Builder(
                    c,
                    recyclerView,
                    viewHolder,
                    dX,
                    dY,
                    actionState,
                    isCurrentlyActive
                )
                    .addSwipeRightBackgroundColor(
                        ContextCompat.getColor(
                            requireContext(),
                            R.color.green
                        )
                    )
                    .addSwipeRightActionIcon(R.drawable.ic_editing_48)
                    .addSwipeRightLabel(getString(R.string.menu_edit))
                    .setSwipeRightLabelColor(Color.WHITE)
                    .setSwipeRightLabelTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)

                    .addSwipeLeftBackgroundColor(Color.RED)
                    .addSwipeLeftActionIcon(R.drawable.ic_delete_48dp)
                    .addSwipeLeftLabel(getString(R.string.menu_remove))
                    .setSwipeLeftLabelColor(Color.WHITE)
                    .setSwipeLeftLabelTextSize(TypedValue.COMPLEX_UNIT_SP, 18f)
                    .create()
                    .decorate()
                super.onChildDraw(
                    c,
                    recyclerView,
                    viewHolder,
                    dX,
                    dY,
                    actionState,
                    isCurrentlyActive
                )
            }
        }).attachToRecyclerView(binding.list)

        itemTouchHelper?.attachToRecyclerView(binding.list)

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.data.collectLatest(adapter::submitData)
            }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                adapter.loadStateFlow.collectLatest { states ->
                    binding.swipeRefresh.isRefreshing =
                        states.refresh is LoadState.Loading
                }
            }
        }

        binding.newPosts.setOnClickListener {
            binding.newPosts.isVisible = false
            viewModel.readAll()
            CoroutineScope(EmptyCoroutineContext).launch {
                delay(100.milliseconds)
                binding.list.post {
                    binding.list.smoothScrollToPosition(0)
                }
            }
        }

        binding.swipeRefresh.setOnRefreshListener(adapter::refresh)

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