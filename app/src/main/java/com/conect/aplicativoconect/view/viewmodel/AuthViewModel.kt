package com.conect.aplicativoconect.view.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.conect.aplicativoconect.view.data.repository.AuthRepository

class AuthViewModel : ViewModel() {
    private val authRepository = AuthRepository()

    fun login(email: String, password: String): LiveData<Boolean> {
        val loginResult = MutableLiveData<Boolean>()
        authRepository.login(email, password) { success ->
            loginResult.value = success
        }
        return loginResult
    }
}
