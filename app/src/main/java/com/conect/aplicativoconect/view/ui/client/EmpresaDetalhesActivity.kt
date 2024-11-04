package com.conect.aplicativoconect.view.ui.client

import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.conect.aplicativoconect.R
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.google.firebase.firestore.FirebaseFirestore

class EmpresaDetalhesActivity : AppCompatActivity() {

    private lateinit var companyId: String
    private lateinit var firestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_about_company)

        // Obter o ID da empresa a partir da intent
        companyId = intent.getStringExtra("companyId") ?: ""
        firestore = FirebaseFirestore.getInstance()

        if (companyId.isNotEmpty()) {
            fetchCompanyDetails(companyId)
            setupTabLayout()
        } else {
            finish()
        }
    }

    private fun setupTabLayout() {
        val viewPager = findViewById<ViewPager2>(R.id.viewPager)
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)

        viewPager.adapter = DetailsPagerAdapter(this, companyId)

        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Serviços"
                1 -> "Fotos"
                2 -> "Avaliação"  // Certifique-se de que "Avaliação" corresponda ao índice correto
                else -> null
            }
        }.attach()
    }

    private fun fetchCompanyDetails(companyId: String) {
        firestore.collection("business").document(companyId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Obtém os dados da empresa
                    val companyName = document.getString("name") ?: ""
                    val companyDescription = document.getString("description") ?: ""
                    val companyImageUrl = document.getString("imageUrl")
                    val profileImageUrl = document.getString("imageUrl")
                    val operatingHoursMap = document.get("operatingHours") as? Map<String, String>
                    val opening = operatingHoursMap?.get("opening") ?: "N/A"
                    val closing = operatingHoursMap?.get("closing") ?: "N/A"
                    val operatingHours = "Horário: $opening - $closing"
                    val category = document.getString("serviceType") ?: "Categoria não informada"

                    // Atualiza as views com os dados da empresa
                    findViewById<TextView>(R.id.companyName).text = companyName
                    findViewById<TextView>(R.id.companyDescription).text = companyDescription
                    findViewById<TextView>(R.id.companyOperatingHours).text = operatingHours
                    findViewById<TextView>(R.id.companyCategory).text = category

                    // Carregar imagens usando Glide
                    val companyImageView = findViewById<ImageView>(R.id.companyImage)
                    val profileImageView = findViewById<ImageView>(R.id.companyProfileImage)
                    loadImage(companyImageUrl, companyImageView)
                    loadImage(profileImageUrl, profileImageView)
                } else {
                    finish()  // Fechar se o documento não existir
                }
            }
            .addOnFailureListener { e ->
                Log.e("EmpresaDetalhesActivity", "Erro ao carregar detalhes", e)
                finish()  // Fechar em caso de erro
            }
    }

    private fun loadImage(imageUrl: String?, imageView: ImageView) {
        if (!imageUrl.isNullOrEmpty()) {
            Glide.with(this)
                .load(imageUrl)
                .thumbnail(0.1f)
                .override(300, 300)
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(imageView)
        } else {
            imageView.setImageResource(R.drawable.foto_perfil_generica)
        }
    }
}
