package ru.netology.nmedia.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import dagger.hilt.android.AndroidEntryPoint
import ru.netology.nmedia.R
import ru.netology.nmedia.databinding.FragmentAuthenticationBinding
import ru.netology.nmedia.viewmodule.AuthViewModel

@AndroidEntryPoint
class AuthenticationFragment : Fragment() {
    private val viewModel: AuthViewModel by viewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        val binding = FragmentAuthenticationBinding.inflate(
            inflater, container, false
        )

        with(binding) {
            btnLogin.setOnClickListener {
                val login = etLogin.text.toString()
                val pass = etPassword.text.toString()
                viewModel.authentication(login, pass)
            }
            btnRegister.setOnClickListener {
                findNavController().navigate(
                    R.id.action_authenticationFragment_to_registrationFragment
                )
            }
        }

        viewModel.authenticationState.observe(viewLifecycleOwner) { result ->
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
                viewModel.authenticationState.value = null
            }
        }

        return binding.root
    }
}