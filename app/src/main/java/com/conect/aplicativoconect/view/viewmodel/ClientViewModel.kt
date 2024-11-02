package com.conect.aplicativoconect.view.viewmodel

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.conect.aplicativoconect.view.data.model.Booking
import com.conect.aplicativoconect.view.data.model.User
import com.conect.aplicativoconect.view.data.repository.UserRepository

class ClientViewModel : ViewModel() {
    private val _userData = MutableLiveData<User?>()
    val userData: LiveData<User?> get() = _userData

    private val _userName = MutableLiveData<String?>()
    val userName: LiveData<String?> get() = _userName

    private val _userEmail = MutableLiveData<String?>() // Nova LiveData para o email
    val userEmail: LiveData<String?> get() = _userEmail

    private val _userImage = MutableLiveData<String?>() // Para armazenar a URL da imagem do usuário
    val userImage: LiveData<String?> get() = _userImage

    private val _todayBookings = MutableLiveData<List<Booking>>() // Mudando para não permitir nulos
    val todayBookings: LiveData<List<Booking>> get() = _todayBookings

    private val _futureBookings =
        MutableLiveData<List<Booking>>() // Mudando para não permitir nulos
    val futureBookings: LiveData<List<Booking>> get() = _futureBookings

    private val userRepository = UserRepository()

    fun loadUserData(userId: String) {
        userRepository.getUserData(userId) { user ->
            _userData.value = user
            user?.let {
                _userName.value = it.name
                _userEmail.value = it.email
                _userImage.value = it.imageUrl
                Log.d(
                    "ClientViewModel",
                    "Image URL: ${it.imageUrl}, Name: ${it.name}, Email: ${it.email}"
                )
            }
        }
    }

    fun setUserName(name: String?) {
        _userName.value = name
    }

    fun setTodayBookings(bookings: List<Booking>) {
        _todayBookings.value = bookings // Agora aceita apenas listas não nulas
    }

}
