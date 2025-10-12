package com.conect.aplicativoconect.utils

import com.google.firebase.auth.FirebaseAuth

object AuthHelper {
    // ✅ OTIMIZADO: Função centralizada para obter userId atual
    fun getCurrentUserId(): String? = FirebaseAuth.getInstance().currentUser?.uid
    
    // ✅ OTIMIZADO: Função com callback para quando userId não existe  
    fun requireCurrentUserId(onMissing: () -> Unit = {}): String? {
        return getCurrentUserId().also { if (it == null) onMissing() }
    }
    
    // ✅ OTIMIZADO: Verificar se usuário está autenticado
    fun isUserAuthenticated(): Boolean = getCurrentUserId() != null
}