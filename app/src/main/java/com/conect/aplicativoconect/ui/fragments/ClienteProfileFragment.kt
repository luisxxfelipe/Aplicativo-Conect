package com.conect.aplicativoconect.ui.fragments

import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.lifecycleScope
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.databinding.FragmentClienteProfileBinding
import com.conect.aplicativoconect.ui.activities.EditProfileActivity
// import com.conect.aplicativoconect.ui.activities.ConfiguracoesActivity
// import com.conect.aplicativoconect.ui.activities.DadosPessoaisActivity
// import com.conect.aplicativoconect.ui.activities.MainActivity  
// import com.conect.aplicativoconect.ui.activities.PerfilCompletoActivity
import com.conect.aplicativoconect.ui.activities.PolicyActivity
import com.conect.aplicativoconect.ui.activities.ReferralPointsActivity
import com.conect.aplicativoconect.ui.viewmodels.ClientViewModel
import com.conect.aplicativoconect.ui.viewmodels.ReferralViewModel
import com.conect.aplicativoconect.utils.AuthHelper
import com.conect.aplicativoconect.utils.ImageHelper
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.launch

class ClienteProfileFragment : Fragment() {

    private var _binding: FragmentClienteProfileBinding? = null
    private val binding get() = _binding!!
    private val clientViewModel: ClientViewModel by activityViewModels()
    private val referralViewModel: ReferralViewModel by activityViewModels()
    private val userId = AuthHelper.getCurrentUserId() // ✅ OTIMIZADO: Helper centralizado

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClienteProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // 🔔 Configuração de notificações será feita em setupNotificationsSwitch()

        // Carregar os dados do usuário
        userId?.let {
            clientViewModel.loadUserData(it)
            // 🎯 NOVO: Carregar pontos do usuário
            referralViewModel.loadUserPoints(it)
        }

        // Observar a imagem do usuário
        clientViewModel.userImage.observe(viewLifecycleOwner) { imageUrl ->
            Log.d("ClienteProfileFragment", "Carregando imagem da URL: $imageUrl")
            
            // 🔧 MELHORIA: Mostrar placeholder enquanto carrega
            if (imageUrl.isNullOrBlank()) {
                // Mostrar imagem padrão imediatamente se não há URL
                binding.profileImageClient.setImageResource(R.drawable.ic_profile_default)
            } else {
                // 🔧 MELHORIA: Carregamento otimizado com placeholder de carregamento
                ImageHelper.loadProfileImage(requireContext(), imageUrl, binding.profileImageClient)
            }
        }

        // Observar o nome do usuário
        clientViewModel.userName.observe(viewLifecycleOwner) { name ->
            binding.userName.text = name
        }

        // Observar o e-mail do usuário
        clientViewModel.userEmail.observe(viewLifecycleOwner) { email ->
            binding.userEmail.text = email
        }
        
        // 🎯 NOVO: Observar pontos do usuário
        referralViewModel.userPoints.observe(viewLifecycleOwner) { userPoints ->
            userPoints?.let {
                binding.pointsValue.text = "${it.totalPoints} pontos"
                binding.referralsCount.text = "${it.referralsCount} indicações"
            } ?: run {
                binding.pointsValue.text = "0 pontos"
                binding.referralsCount.text = "0 indicações"
            }
        }

        // Ação do botão de editar dados pessoais
        binding.personalDataButton.setOnClickListener {
            val intent = Intent(requireContext(), EditProfileActivity::class.java)
            startActivity(intent)
        }
        
        // 🎯 NOVO: Ação do botão de pontos e cupons
        binding.pointsButton.setOnClickListener {
            val intent = Intent(requireContext(), ReferralPointsActivity::class.java)
            startActivity(intent)
        }

        // Ação do botão de ajuda via WhatsApp
        binding.helpButton.setOnClickListener {
            val message = "Olá, preciso de ajuda com o aplicativo."
            val url = "https://wa.me/5535984478656?text=${Uri.encode(message)}"
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            startActivity(intent)
        }

        // Ação do botão sobre as políticas
        binding.aboutPolicy.setOnClickListener {
            val intent = Intent(requireContext(), PolicyActivity::class.java)
            startActivity(intent)
        }

        // 🔔 NOTIFICAÇÕES: Configuração melhorada
        setupNotificationsSwitch()
    }

    /**
     * 🔔 CONFIGURAÇÃO AVANÇADA DE NOTIFICAÇÕES
     */
    private fun setupNotificationsSwitch() {
        val sharedPreferences = requireContext().getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        val isNotificationsEnabled = sharedPreferences.getBoolean("notifications_enabled", true) // Padrão: ativado
        
        // Definir estado inicial
        binding.notificationsSwitch.isChecked = isNotificationsEnabled
        updateNotificationSwitchVisuals(isNotificationsEnabled)
        
        // Listener para mudanças
        binding.notificationsSwitch.setOnCheckedChangeListener { switch, isChecked ->
            // 🎨 Feedback visual imediato
            updateNotificationSwitchVisuals(isChecked)
            
            // 💾 Salvar preferência
            val editor = sharedPreferences.edit()
            editor.putBoolean("notifications_enabled", isChecked)
            editor.apply()
            
            // 🔥 Firebase Messaging
            if (isChecked) {
                enableNotifications()
            } else {
                disableNotifications()
            }
        }
    }
    
    /**
     * 🎨 ATUALIZAR VISUAL DO SWITCH
     */
    private fun updateNotificationSwitchVisuals(isEnabled: Boolean) {
        binding.notificationsSwitch.apply {
            // Atualizar cor do texto baseado no estado
            setTextColor(
                if (isEnabled) {
                    ContextCompat.getColor(requireContext(), R.color.textPrimary)
                } else {
                    ContextCompat.getColor(requireContext(), R.color.cinza)
                }
            )
            
            // O texto já está definido no layout XML
        }
    }
    
    /**
     * 🔔 ATIVAR NOTIFICAÇÕES
     */
    private fun enableNotifications() {
        FirebaseMessaging.getInstance().subscribeToTopic("all_users")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // 🔔 Também subscrever para notificações específicas do usuário
                    userId?.let { id ->
                        FirebaseMessaging.getInstance().subscribeToTopic("user_$id")
                    }
                    
                    Toast.makeText(
                        requireContext(), 
                        "✅ Notificações ativadas com sucesso!", 
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    Log.d("ClienteProfile", "Notificações ativadas com sucesso")
                } else {
                    Log.e("ClienteProfile", "Erro ao ativar notificações: ${task.exception?.message}")
                    
                    // Reverter o switch em caso de erro
                    binding.notificationsSwitch.isChecked = false
                    updateNotificationSwitchVisuals(false)
                    
                    Toast.makeText(
                        requireContext(),
                        "❌ Erro ao ativar notificações. Tente novamente.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }
    
    /**
     * 🔕 DESATIVAR NOTIFICAÇÕES
     */
    private fun disableNotifications() {
        FirebaseMessaging.getInstance().unsubscribeFromTopic("all_users")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    // 🔕 Também desinscrever das notificações específicas do usuário
                    userId?.let { id ->
                        FirebaseMessaging.getInstance().unsubscribeFromTopic("user_$id")
                    }
                    
                    Toast.makeText(
                        requireContext(),
                        "🔕 Notificações desativadas",
                        Toast.LENGTH_SHORT
                    ).show()
                    
                    Log.d("ClienteProfile", "Notificações desativadas com sucesso")
                } else {
                    Log.e("ClienteProfile", "Erro ao desativar notificações: ${task.exception?.message}")
                    
                    // Reverter o switch em caso de erro
                    binding.notificationsSwitch.isChecked = true
                    updateNotificationSwitchVisuals(true)
                    
                    Toast.makeText(
                        requireContext(),
                        "❌ Erro ao desativar notificações. Tente novamente.",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
