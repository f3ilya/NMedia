package ru.netology.nmedia.fragment

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.net.toFile
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.github.dhaval2404.imagepicker.ImagePicker
import com.github.dhaval2404.imagepicker.constant.ImageProvider
import com.google.android.material.snackbar.Snackbar
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.FragmentRegistrationBinding
import ru.netology.nmedia.util.showConfirmationDialog
import ru.netology.nmedia.viewmodule.RegViewModel

@AndroidEntryPoint
class RegistrationFragment : Fragment() {
    private val viewModel: RegViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentRegistrationBinding.inflate(
            inflater, container, false
        )

        val pickPhotoLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
            when (it.resultCode) {
                ImagePicker.RESULT_ERROR -> {
                    Snackbar.make(
                        binding.root,
                        ImagePicker.getError(it.data),
                        Snackbar.LENGTH_LONG
                    ).show()
                }
                Activity.RESULT_OK -> {
                    val uri: Uri? = it.data?.data
                    viewModel.changePhoto(uri, uri?.toFile())
                }
            }
        }

        with(binding) {
            btnCreateAccount.setOnClickListener {
                val name = etName.text.toString()
                val login = etLogin.text.toString()
                val pass = etPassword.text.toString()
                val confPass = etConfirmPassword.text.toString()

                if (name.isEmpty() || login.isEmpty() || pass.isEmpty() || confPass.isEmpty()) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.all_fields_are_required),
                        Toast.LENGTH_LONG
                    ).show()
                    return@setOnClickListener
                }
                if (pass != confPass) {
                    Toast.makeText(
                        requireContext(),
                        getString(R.string.passwords_don_t_match),
                        Toast.LENGTH_LONG
                    ).show()
                    return@setOnClickListener
                }
                viewModel.registration(login, pass, name)
            }
            btnBackToLogin.setOnClickListener {
                findNavController().navigate(
                    R.id.action_registrationFragment_to_authenticationFragment
                )
            }
            ivAvatar.setOnClickListener {
                requireContext().showConfirmationDialog(
                    title = getString(R.string.choose_a_profile_picture),
                    onConfirm = fun() {
                        ImagePicker.with(requireActivity())
                            .crop()
                            .compress(2048)
                            .provider(ImageProvider.GALLERY)
                            .galleryMimeTypes(
                                arrayOf(
                                    "image/png",
                                    "image/jpeg",
                                )
                            ).createIntent(pickPhotoLauncher::launch)
                    },
                    positive = getString(R.string.description_select_photo),
                    negative = getString(R.string.description_take_photo),
                    onCancel = fun() {
                        ImagePicker.with(requireActivity())
                            .crop()
                            .compress(2048)
                            .provider(ImageProvider.CAMERA)
                            .createIntent(pickPhotoLauncher::launch)
                    },
                    neutral = getString(R.string.remove),
                    onNeutral = fun() {
                        viewModel.changePhoto(null, null)
                        ivAvatar.setImageResource(android.R.drawable.sym_def_app_icon)
                    },
                    isNeed = viewModel.photo.value?.file != null
                )
            }
        }

        viewModel.photo.observe(viewLifecycleOwner) {
            binding.ivAvatar.setImageURI(it.uri)
        }

        viewModel.registrationState.observe(viewLifecycleOwner) { result ->
            result?.let {
                if (it.isSuccess) {
                    val startDestinationId = findNavController().graph.startDestinationId
                    findNavController().popBackStack(startDestinationId, false)
                } else {
                    val message = it.exceptionOrNull()?.message
                    Toast.makeText(
                        requireContext(),
                        message,
                        Toast.LENGTH_LONG
                    ).show()
                }
                viewModel.registrationState.value = null
            }
        }

        return binding.root
    }
}