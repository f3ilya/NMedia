package ru.netology.nmedia.viewmodule

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import ru.netology.nmedia.R
import ru.netology.nmedia.api.Api
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.auth.AuthState

class AuthViewModel : ViewModel() {
    val data: LiveData<AuthState> = AppAuth.getInstance()
        .authStateFlow
        .asLiveData(Dispatchers.Default)
    val authenticated: Boolean
        get() = AppAuth.getInstance().authStateFlow.value.id != 0L
    val authenticationState = MutableLiveData<Result<Unit>?>()

    fun authentication(login: String, pass: String) {
        viewModelScope.launch {
            runCatching {
                val response = Api.retrofitService.authenticationUser(login, pass)
                if (response.isSuccessful) {
                    response.body()?.let {
                        AppAuth.getInstance().setAuth(it.id, it.token)
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