package com.conect.aplicativoconect.ui.viewmodels

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.conect.aplicativoconect.data.models.Coupon
import com.conect.aplicativoconect.data.models.UserPoints
import com.conect.aplicativoconect.data.repositories.ReferralRepository
import kotlinx.coroutines.launch

class ReferralViewModel : ViewModel() {
    
    private val repository = ReferralRepository()
    
    private val _userPoints = MutableLiveData<UserPoints?>()
    val userPoints: LiveData<UserPoints?> = _userPoints
    
    private val _userCoupons = MutableLiveData<List<Coupon>>()
    val userCoupons: LiveData<List<Coupon>> = _userCoupons
    
    private val _loading = MutableLiveData<Boolean>()
    val loading: LiveData<Boolean> = _loading
    
    private val _error = MutableLiveData<String?>()
    val error: LiveData<String?> = _error
    
    private val _couponRedeemed = MutableLiveData<Coupon?>()
    val couponRedeemed: LiveData<Coupon?> = _couponRedeemed
    
    fun loadUserPoints(userId: String? = null) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val points = if (userId != null) {
                    repository.getUserPoints(userId)
                } else {
                    repository.getUserPoints()
                }
                _userPoints.value = points
                
                // Carregar cupons também
                val coupons = repository.getUserCoupons(userId ?: "")
                _userCoupons.value = coupons
                
            } catch (e: Exception) {
                _error.value = "Erro ao carregar pontos: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
    
    fun redeemCoupon(pointsCost: Int, discountPercent: Int) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val coupon = repository.redeemCoupon(pointsCost, discountPercent)
                if (coupon != null) {
                    _couponRedeemed.value = coupon
                    // Recarregar pontos atualizados
                    loadUserPoints()
                } else {
                    _error.value = "Pontos insuficientes ou erro ao resgatar cupom"
                }
            } catch (e: Exception) {
                _error.value = "Erro ao resgatar cupom: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
    
    fun generateReferralMessage(userName: String): String {
        return repository.generateReferralMessage(userName)
    }
    
    fun clearError() {
        _error.value = null
    }
    
    fun clearCouponRedeemed() {
        _couponRedeemed.value = null
    }
    
    // 🔒 ===== MÉTODOS DE CONTROLE DIÁRIO =====
    
    /**
     * Verifica se deve mostrar popup de indicações (controle de 24h)
     */
    suspend fun shouldShowReferralPopup(userId: String): Boolean {
        return repository.shouldShowReferralPopup(userId)
    }
    
    /**
     * Atualiza timestamp do último popup mostrado
     */
    suspend fun updateLastPopupShown(userId: String) {
        repository.updateLastPopupShown(userId)
    }
    
    /**
     * Verifica limite diário de indicações
     */
    suspend fun checkDailyReferralLimit(userId: String): Boolean {
        return repository.checkDailyReferralLimit(userId)
    }
    
    /**
     * Adiciona nova indicação com validação anti-fraud
     */
    fun addReferralWithValidation(
        referredUserName: String, 
        deviceInfo: String, 
        ipAddress: String
    ) {
        viewModelScope.launch {
            _loading.value = true
            try {
                val success = repository.addReferral(referredUserName)
                
                if (success) {
                    // Recarregar pontos
                    loadUserPoints()
                } else {
                    _error.value = "Erro ao adicionar indicação. Tente novamente."
                }
            } catch (e: Exception) {
                _error.value = "Erro ao processar indicação: ${e.message}"
            } finally {
                _loading.value = false
            }
        }
    }
}