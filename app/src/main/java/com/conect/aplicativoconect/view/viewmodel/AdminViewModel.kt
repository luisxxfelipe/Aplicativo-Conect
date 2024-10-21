package com.conect.aplicativoconect.view.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class AdminViewModel : ViewModel() {
    private val _todayBookingsCount = MutableLiveData<Int>()
    val todayBookingsCount: LiveData<Int> get() = _todayBookingsCount

    private val _monthBookingsCount = MutableLiveData<Int>()
    val monthBookingsCount: LiveData<Int> get() = _monthBookingsCount

    private val _todayProfit = MutableLiveData<Double>()
    val todayProfit: LiveData<Double> get() = _todayProfit

    private val _monthProfit = MutableLiveData<Double>()
    val monthProfit: LiveData<Double> get() = _monthProfit

    private val _businessName = MutableLiveData<String>()
    val businessName: LiveData<String> get() = _businessName

    private val _adminName = MutableLiveData<String>() // Nome do administrador
    val adminName: LiveData<String> get() = _adminName

    private val _adminEmail = MutableLiveData<String>() // E-mail do administrador
    val adminEmail: LiveData<String> get() = _adminEmail

    private val firestore: FirebaseFirestore = Firebase.firestore

    // Métodos para atualizar dados
    fun setTodayBookingsCount(count: Int) {
        _todayBookingsCount.value = count
    }

    fun setMonthBookingsCount(count: Int) {
        _monthBookingsCount.value = count
    }

    fun setTodayProfit(profit: Double) {
        _todayProfit.value = profit
    }

    fun setMonthProfit(profit: Double) {
        _monthProfit.value = profit
    }

    fun setBusinessName(name: String) {
        _businessName.value = name
    }

    fun setAdminName(name: String) {
        _adminName.value = name
    }

    fun setAdminEmail(email: String) {
        _adminEmail.value = email
    }

    // Método para carregar dados do administrador
    fun loadAdminData() {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        firestore.collection("business") // Nome da coleção no Firestore
            .document(currentUserUid) // Use o UID do usuário autenticado
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val name = document.getString("name") // Campo 'name' no Firestore
                    val email = document.getString("email") // Campo 'email' no Firestore
                    setAdminName(name ?: "Nome não disponível")
                    setAdminEmail(email ?: "Email não disponível")
                } else {
                    setAdminName("Nome não disponível")
                    setAdminEmail("Email não disponível")
                }
            }
            .addOnFailureListener { exception ->
                // Tratar erro
                setAdminName("Erro ao carregar nome")
                setAdminEmail("Erro ao carregar email")
            }
    }
}
