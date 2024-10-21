package com.conect.aplicativoconect.view.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.conect.aplicativoconect.view.data.model.Booking
import com.conect.aplicativoconect.view.data.model.User
import com.conect.aplicativoconect.view.data.repository.UserRepository

class ClientViewModel : ViewModel() {
    private val _userData = MutableLiveData<User?>()
    val userData: LiveData<User?> get() = _userData

    private val userRepository = UserRepository()

    fun loadUserData(userId: String) {
        userRepository.getUserData(userId) { user ->
            _userData.value = user
        }
    }

    private val _userName = MutableLiveData<String?>()
    val userName: LiveData<String?> get() = _userName

    private val _todayBookings = MutableLiveData<List<Booking>>() // Mudando para não permitir nulos
    val todayBookings: LiveData<List<Booking>> get() = _todayBookings

    private val _futureBookings = MutableLiveData<List<Booking>>() // Mudando para não permitir nulos
    val futureBookings: LiveData<List<Booking>> get() = _futureBookings

    fun setUserName(name: String?) {
        _userName.value = name
    }

    fun setTodayBookings(bookings: List<Booking>) {
        _todayBookings.value = bookings // Agora aceita apenas listas não nulas
    }

    fun setFutureBookings(bookings: List<Booking>) {
        _futureBookings.value = bookings // Agora aceita apenas listas não nulas
    }
}
