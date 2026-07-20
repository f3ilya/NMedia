package ru.netology.nmedia.viewmodule

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.netology.nmedia.R
import ru.netology.nmedia.api.ApiService
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.auth.AuthState
import javax.inject.Inject

@HiltViewModel
class AuthViewModel @Inject constructor(
    private val auth: AppAuth,
    private val api: ApiService,
) : ViewModel() {
    val data: LiveData<AuthState> = auth.authStateFlow
        .asLiveData(Dispatchers.Default)
    val authenticated: Boolean
        get() = auth.authStateFlow.value.id != 0L
    val authenticationState = MutableLiveData<Result<Unit>?>()

    fun authentication(login: String, pass: String) {
        viewModelScope.launch {
            runCatching {
                val response = api.authenticationUser(login, pass)
                if (response.isSuccessful) {
                    response.body()?.let {
                        auth.setAuth(it.id, it.token)
                        failOrSuccess()
                    } ?: run {
                        failOrSuccess(R.string.empty_response_from_the_server.toString())
                    }
                } else {
                    failOrSuccess(R.string.incorrect_login_or_password.toString())
                }
            }.onFailure {
                failOrSuccess(R.string.network_or_server_error.toString())
            }
        }
    }

    fun failOrSuccess(message: String? = null) {
        authenticationState.value =
            if (message.isNullOrEmpty()) Result.success(Unit) else Result.failure(Exception(message))
    }
}