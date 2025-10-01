package com.conect.aplicativoconect.ui.viewmodels

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

    private val _companyId = MutableLiveData<String>() // ID da empresa
    val companyId: LiveData<String> get() = _companyId

    private val _address = MutableLiveData<String>() // Endereço da empresa
    val address: LiveData<String> get() = _address

    private val _operatingHours = MutableLiveData<String>() // Horário de funcionamento
    val operatingHours: LiveData<String> get() = _operatingHours

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

    fun setCompanyId(id: String) {
        _companyId.value = id
    }

    fun setAddress(addr: String) {
        _address.value = addr
    }

    fun setOperatingHours(hours: String) {
        _operatingHours.value = hours
    }

    // Metodo para carregar dados do administrador
    fun loadAdminData() {
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: return
        firestore.collection("business")
            .document(currentUserUid)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val name = document.getString("name") ?: "Nome não disponível"
                    val email = document.getString("email") ?: "Email não disponível"
                    val companyId = document.getString("companyId") ?: "ID não disponível"
                    val address = document.getString("address") ?: "Endereço não disponível"

                    // Horário de funcionamento
                    val operatingHoursMap = document.get("operatingHours") as? Map<*, *>
                    val opening = operatingHoursMap?.get("opening") as? String ?: "00:00"
                    val closing = operatingHoursMap?.get("closing") as? String ?: "00:00"

                    val operatingHours = "$opening - $closing"

                    // Atualiza os LiveData
                    setBusinessName(name)
                    setAdminName(name)
                    setAdminEmail(email)
                    setCompanyId(companyId)
                    setAddress(address)
                    setOperatingHours(operatingHours)
                } else {
                    setAdminName("Nome não disponível")
                    setAdminEmail("Email não disponível")
                    setCompanyId("ID não disponível")
                    setAddress("Endereço não disponível")
                    setOperatingHours("Horário não disponível")
                }
            }
            .addOnFailureListener {
                setAdminName("Erro ao carregar nome")
                setAdminEmail("Erro ao carregar email")
                setCompanyId("Erro ao carregar ID")
                setAddress("Erro ao carregar endereço")
                setOperatingHours("Erro ao carregar horários")
            }
    }
}
