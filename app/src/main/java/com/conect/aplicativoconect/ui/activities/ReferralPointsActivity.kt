package com.conect.aplicativoconect.ui.activities

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.databinding.ActivityReferralPointsBinding
import com.conect.aplicativoconect.ui.adapters.CouponAdapter
import com.conect.aplicativoconect.ui.viewmodels.ReferralViewModel
import com.conect.aplicativoconect.utils.AuthHelper

/**
 * 🎯 ACTIVITY: Meus Pontos e Cupons
 * Tela para gerenciar pontos de indicação e resgatar cupons
 */
class ReferralPointsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityReferralPointsBinding
    private lateinit var referralViewModel: ReferralViewModel
    private lateinit var couponAdapter: CouponAdapter
    private val userId = AuthHelper.getCurrentUserId()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityReferralPointsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // ✅ SETUP
        setupToolbar()
        setupViewModel()
        setupRecyclerView()
        setupClickListeners()
        loadUserData()
    }

    private fun setupToolbar() {
        setSupportActionBar(binding.toolbar)
        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            setDisplayShowHomeEnabled(true)
            title = "Meus Pontos e Cupons"
        }
    }

    private fun setupViewModel() {
        referralViewModel = ViewModelProvider(this)[ReferralViewModel::class.java]

        // 🔍 OBSERVAR DADOS
        referralViewModel.userPoints.observe(this) { userPoints ->
            userPoints?.let { points ->
                binding.tvTotalPoints.text = "${points.totalPoints} pontos"
                binding.tvReferralsCount.text = "👥 ${points.referralsCount} indicações realizadas"
            }
        }

        referralViewModel.userCoupons.observe(this) { coupons ->
            couponAdapter.updateCoupons(coupons)
            binding.emptyState.visibility = if (coupons.isEmpty()) {
                android.view.View.VISIBLE
            } else {
                android.view.View.GONE
            }
        }

        referralViewModel.loading.observe(this) { isLoading ->
            // O layout atual não tem progressBar, vamos comentar
            // binding.progressBar.visibility = if (isLoading) {
            //     android.view.View.VISIBLE
            // } else {
            //     android.view.View.GONE
            // }
        }

        referralViewModel.error.observe(this) { errorMessage ->
            errorMessage?.let {
                Toast.makeText(this, it, Toast.LENGTH_LONG).show()
                referralViewModel.clearError()
            }
        }

        referralViewModel.couponRedeemed.observe(this) { coupon ->
            coupon?.let {
                Toast.makeText(
                    this,
                    "🎉 Cupom ${it.code} resgatado com sucesso!",
                    Toast.LENGTH_LONG
                ).show()
                referralViewModel.clearCouponRedeemed()
            }
        }
    }

    private fun setupRecyclerView() {
        couponAdapter = CouponAdapter { coupon ->
            // TODO: Implementar uso do cupom
            Toast.makeText(
                this,
                "Cupom ${coupon.code} copiado para área de transferência!",
                Toast.LENGTH_SHORT
            ).show()
        }

        binding.rvAvailableCoupons.apply {
            adapter = couponAdapter
            layoutManager = LinearLayoutManager(this@ReferralPointsActivity)
        }
    }

    private fun setupClickListeners() {
        // 📱 Compartilhar no WhatsApp
        binding.btnShareApp.setOnClickListener {
            shareReferralMessage()
        }
        
        // 🎯 Botões de resgate de cupons
        binding.btnRedeem10.setOnClickListener {
            redeemCoupon(500, 10)
        }

        binding.btnRedeem20.setOnClickListener {
            redeemCoupon(1000, 20)
        }

        binding.btnRedeem50.setOnClickListener {
            redeemCoupon(2000, 50)
        }
    }

    private fun loadUserData() {
        userId?.let { id ->
            referralViewModel.loadUserPoints(id)
        } ?: run {
            Toast.makeText(this, "Erro: Usuário não autenticado", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun redeemCoupon(pointsCost: Int, discountPercent: Int) {
        val currentPoints = referralViewModel.userPoints.value?.totalPoints ?: 0

        if (currentPoints >= pointsCost) {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Resgatar Cupom")
                .setMessage("Deseja resgatar um cupom de $discountPercent% de desconto por $pointsCost pontos?")
                .setPositiveButton("Sim") { _, _ ->
                    referralViewModel.redeemCoupon(pointsCost, discountPercent)
                }
                .setNegativeButton("Cancelar", null)
                .show()
        } else {
            val pointsNeeded = pointsCost - currentPoints
            Toast.makeText(
                this,
                "Você precisa de mais $pointsNeeded pontos para resgatar este cupom",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun shareReferralMessage() {
        val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        val userName = currentUser?.displayName ?: "Usuário"
        val appUrl = "https://play.google.com/store/apps/details?id=com.conect.aplicativoconect"
        val message = "🔥 Olá! Sou $userName e indico o app *Conect*!\n\n" +
                "📱 Encontre os melhores profissionais da sua região de forma rápida e segura.\n\n" +
                "✨ Baixe agora: $appUrl\n\n" +
                "#Conect #Profissionais #Indicação"

        val whatsappIntent = android.content.Intent().apply {
            action = android.content.Intent.ACTION_SEND
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, message)
            setPackage("com.whatsapp")
        }

        try {
            startActivity(whatsappIntent)
        } catch (e: Exception) {
            // Fallback para compartilhamento geral
            val shareIntent = android.content.Intent().apply {
                action = android.content.Intent.ACTION_SEND
                type = "text/plain"
                putExtra(android.content.Intent.EXTRA_TEXT, message)
            }
            startActivity(android.content.Intent.createChooser(shareIntent, "Compartilhar indicação"))
        }
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}