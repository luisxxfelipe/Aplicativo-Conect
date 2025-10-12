package com.conect.aplicativoconect.utils

import android.content.Context
import android.util.Log
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.conect.aplicativoconect.R

object ImageHelper {
    
    // 🔧 FIX CRÍTICO: Limpar qualquer carregamento pendente
    fun clearImage(context: Context, imageView: ImageView) {
        try {
            Glide.with(context).clear(imageView)
            imageView.setImageResource(R.drawable.ic_profile_default)
        } catch (e: Exception) {
            Log.e("ImageHelper", "Erro ao limpar imagem: ${e.message}")
        }
    }
    // 🔧 FIX CRÍTICO: Carregamento seguro de imagens de perfil
    fun loadProfileImage(
        context: Context, 
        imageUrl: String?, 
        imageView: ImageView,
        placeholder: Int = R.drawable.ic_profile_default,
        errorImage: Int = R.drawable.ic_profile_default
    ) {
        try {
            // 🔧 LOG DE DEBUG para identificar problemas
            Log.d("ImageHelper", "Tentando carregar imagem: '$imageUrl'")
            
            // 🔧 VERIFICAÇÕES MÚLTIPLAS: Evitar qualquer tentativa de carregar URL inválida
            if (imageUrl.isNullOrBlank() || 
                imageUrl == "null" || 
                imageUrl == "undefined" || 
                imageUrl.trim().isEmpty()) {
                
                Log.d("ImageHelper", "URL inválida ou vazia, limpando e usando imagem padrão")
                // 🔧 LIMPAR primeiro qualquer carregamento pendente
                clearImage(context, imageView)
                return
            }

            // 🔧 VALIDAR se a URL é válida antes de tentar carregar
            if (!imageUrl.startsWith("http://") && 
                !imageUrl.startsWith("https://") && 
                !imageUrl.startsWith("file://") && 
                !imageUrl.startsWith("content://")) {
                
                Log.d("ImageHelper", "URL com protocolo inválido, limpando e usando imagem padrão")
                clearImage(context, imageView)
                return
            }

            // 🔧 CARREGAMENTO OTIMIZADO com transição suave
            Glide.with(context)
                .load(imageUrl)
                .placeholder(placeholder)
                .error(errorImage)
                .fallback(errorImage) // ⚡ FIX: Fallback extra para casos não previstos
                .override(300, 300) // ⚡ REDUZIDO: Menor resolução para performance
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .centerCrop() // 🔧 MELHORIA: Crop centralizado
                .dontAnimate() // 🔧 PERFORMANCE: Sem animação desnecessária
                .into(imageView)
                
        } catch (e: Exception) {
            // 🔧 FALLBACK COMPLETO: Em caso de qualquer erro, usar imagem padrão
            try {
                imageView.setImageResource(placeholder)
            } catch (ex: Exception) {
                // Ignore - último recurso
            }
        }
    }
    
    // 🔧 FIX CRÍTICO: Carregamento seguro para business images  
    fun loadBusinessImage(
        context: Context,
        imageUrl: String?,
        imageView: ImageView,
        width: Int = 300,
        height: Int = 200
    ) {
        try {
            // 🔧 VERIFICAÇÕES MÚLTIPLAS: Evitar qualquer tentativa de carregar URL inválida
            if (imageUrl.isNullOrBlank() || 
                imageUrl == "null" || 
                imageUrl == "undefined" || 
                imageUrl.trim().isEmpty()) {
                
                imageView.setImageResource(R.drawable.ic_profile_default)
                return
            }

            // 🔧 VALIDAR se a URL é válida antes de tentar carregar
            if (!imageUrl.startsWith("http://") && 
                !imageUrl.startsWith("https://") && 
                !imageUrl.startsWith("file://") && 
                !imageUrl.startsWith("content://")) {
                
                imageView.setImageResource(R.drawable.ic_profile_default)
                return
            }

            // 🔧 CARREGAMENTO SEGURO com Glide
            Glide.with(context)
                .load(imageUrl)
                .placeholder(R.drawable.ic_profile_default)
                .error(R.drawable.ic_profile_default)
                .fallback(R.drawable.ic_profile_default) // ⚡ FIX: Fallback extra
                .override(width, height)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(imageView)
                
        } catch (e: Exception) {
            // 🔧 FALLBACK COMPLETO: Em caso de qualquer erro, usar imagem padrão
            try {
                imageView.setImageResource(R.drawable.ic_profile_default)
            } catch (ex: Exception) {
                // Ignore - último recurso
            }
        }
    }
}