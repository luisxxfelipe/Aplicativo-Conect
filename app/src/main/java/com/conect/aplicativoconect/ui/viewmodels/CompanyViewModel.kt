package com.conect.aplicativoconect.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.conect.aplicativoconect.data.models.Business
import com.conect.aplicativoconect.data.models.Service
import com.conect.aplicativoconect.data.models.ServicePhoto
import com.conect.aplicativoconect.data.repositories.CompanyRepository
import kotlinx.coroutines.launch

class CompanyViewModel : ViewModel() {
    
    private val repository = CompanyRepository()
    
    // Loading state
    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading
    
    // Error state  
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    private val _company = MutableLiveData<Business?>()
    val company: LiveData<Business?> = _company
    
    private val _services = MutableLiveData<List<Service>>()
    val services: LiveData<List<Service>> = _services
    
    private val _photos = MutableLiveData<List<ServicePhoto>>()
    val photos: LiveData<List<ServicePhoto>> = _photos
    
    private val _reviews = MutableLiveData<List<ReviewItem>>()
    val reviews: LiveData<List<ReviewItem>> = _reviews
    
    fun loadCompanyDetails(companyId: String) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val companyData = repository.getCompanyById(companyId)
                _company.value = companyData
                _error.value = null
            } catch (e: Exception) {
                _error.value = "Erro ao carregar empresa: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    fun loadServices(companyId: String) {
        viewModelScope.launch {
            try {
                val servicesList = repository.getCompanyServices(companyId)
                _services.value = servicesList
            } catch (e: Exception) {
                _error.value = "Erro ao carregar serviços: ${e.message}"
            }
        }
    }
    
    fun loadPhotos(companyId: String) {
        viewModelScope.launch {
            try {
                val photosList = repository.getCompanyPhotos(companyId)
                _photos.value = photosList
            } catch (e: Exception) {
                _error.value = "Erro ao carregar fotos: ${e.message}"
            }
        }
    }
    
    fun loadReviews(companyId: String) {
        viewModelScope.launch {
            try {
                val reviewsList = repository.getCompanyReviews(companyId)
                _reviews.value = reviewsList
            } catch (e: Exception) {
                _error.value = "Erro ao carregar avaliações: ${e.message}"
            }
        }
    }
    
    // Operating Hours
    private val _operatingHours = MutableLiveData<CompanyRepository.OperatingHours?>()
    val operatingHours: LiveData<CompanyRepository.OperatingHours?> = _operatingHours
    
    fun loadOperatingHours(companyId: String) {
        viewModelScope.launch {
            try {
                val hours = repository.getOperatingHours(companyId)
                _operatingHours.value = hours
            } catch (e: Exception) {
                _error.value = "Erro ao carregar horários: ${e.message}"
            }
        }
    }
    
    fun saveOperatingHours(companyId: String, operatingHours: CompanyRepository.OperatingHours) {
        viewModelScope.launch {
            _isLoading.value = true
            try {
                val success = repository.saveOperatingHours(companyId, operatingHours)
                if (success) {
                    _operatingHours.value = operatingHours
                } else {
                    _error.value = "Erro ao salvar horários de funcionamento"
                }
            } catch (e: Exception) {
                _error.value = "Erro ao salvar horários: ${e.message}"
            } finally {
                _isLoading.value = false
            }
        }
    }
    
    data class ReviewItem(
        val name: String,
        val comment: String,
        val quality: Long,
        val punctuality: Long,
        val service: Long
    )
}