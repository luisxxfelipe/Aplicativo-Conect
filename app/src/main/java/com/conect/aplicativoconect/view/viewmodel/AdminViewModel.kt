package com.conect.aplicativoconect.view.viewmodel

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel

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
}
