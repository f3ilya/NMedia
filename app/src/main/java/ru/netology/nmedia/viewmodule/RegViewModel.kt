package ru.netology.nmedia.viewmodule

import android.net.Uri
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import ru.netology.nmedia.R
import ru.netology.nmedia.api.Api
import ru.netology.nmedia.auth.AppAuth
import ru.netology.nmedia.auth.AuthState
import ru.netology.nmedia.model.PhotoModel
import java.io.File

private val noPhoto = PhotoModel()
class RegViewModel : ViewModel() {
    val data: LiveData<AuthState> = AppAuth.getInstance()
        .authStateFlow
        .asLiveData(Dispatchers.Default)
    val registrationState = MutableLiveData<Result<Unit>?>()
    private val _photo = MutableLiveData(noPhoto)
    val photo: LiveData<PhotoModel>
        get() = _photo

    fun registration(login: String, pass: String, name: String) {
        viewModelScope.launch {
            runCatching {
                val response = when(_photo.value) {
                    noPhoto -> Api.retrofitService.registrationUser(login, pass, name)
                    else -> _photo.value?.file?.let { file ->
                        Api.retrofitService.registrationWithPhoto(
                            login.toRequestBody("text/plain".toMediaType()),
                            pass.toRequestBody("text/plain".toMediaType()),
                            name.toRequestBody("text/plain".toMediaType()),
                            MultipartBody.Part.createFormData(
                                "file", file.name, file.asRequestBody()
                            )
                        )
                    }
                }
                if (response?.isSuccessful == true) {
                    response.body()?.let {
                        AppAuth.getInstance().setAuth(it.id, it.token)
                        failOrSuccess()
                    } ?: run {
                        failOrSuccess(R.string.empty_response_from_the_server.toString())
                    }
                } else {
                    failOrSuccess(R.string.login_is_already_taken.toString())
                }
            }.onFailure {
                failOrSuccess(R.string.network_or_server_error.toString())
            }
        }
    }

    fun failOrSuccess(message: String? = null) {
        registrationState.value =
            if (message.isNullOrEmpty()) Result.success(Unit) else Result.failure(Exception(message))
    }

    fun changePhoto(uri: Uri?, file: File?) {
        _photo.value = PhotoModel(uri, file)
    }
}