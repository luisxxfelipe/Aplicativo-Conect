package com.conect.aplicativoconect.view.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.conect.aplicativoconect.view.data.model.Booking
import com.conect.aplicativoconect.view.data.model.User // Importar o modelo de User
import com.conect.aplicativoconect.view.data.repository.UserRepository // Importe o repositório correspondente

class ClientViewModel : ViewModel() {
    private val _userName = MutableLiveData<String>()
    val userName: LiveData<String> get() = _userName

    private val _todayBookings = MutableLiveData<List<Booking>>()
    val todayBookings: LiveData<List<Booking>> get() = _todayBookings

    private val _futureBookings = MutableLiveData<List<Booking>>()
    val futureBookings: LiveData<List<Booking>> get() = _futureBookings

    private val _userData = MutableLiveData<User>() // Adicione o LiveData para os dados do usuário
    val userData: LiveData<User> get() = _userData

    private val userRepository = UserRepository() // Certifique-se de que seu repositório está configurado

    fun setUserName(name: String) {
        _userName.value = name
    }

    fun setTodayBookings(bookings: List<Booking>) {
        _todayBookings.value = bookings
    }

    fun setFutureBookings(bookings: List<Booking>) {
        _futureBookings.value = bookings
    }

    fun loadUserData(userId: String) {
        userRepository.getUserData(userId) { user ->
            _userData.value = user // Atualiza o LiveData com os dados do usuário
        }
    }
}
