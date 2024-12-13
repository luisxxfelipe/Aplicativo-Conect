package com.conect.aplicativoconect.view.ui.client

import android.content.res.Configuration
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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

        // Usando TabLayoutMediator para sincronizar com o ViewPager
        TabLayoutMediator(tabLayout, viewPager) { tab, position ->
            tab.text = when (position) {
                0 -> "Serviços"
                1 -> "Fotos"
                2 -> "Avaliação"
                else -> null
            }
        }.attach()

        // Verificar o tema atual (modo claro ou escuro)
        val isDarkMode =
            resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK == Configuration.UI_MODE_NIGHT_YES

        // Adiciona o listener para detectar as abas selecionadas
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                tab?.let {
                    val tabView = it.view

                    // Definindo o fundo da aba dependendo do tema
                    if (isDarkMode) {
                        // No modo escuro, o fundo da aba deve ser transparente
                        tabView.setBackgroundColor(
                            ContextCompat.getColor(
                                this@EmpresaDetalhesActivity,
                                android.R.color.transparent
                            )
                        )
                    } else {
                        // No modo claro, o fundo será roxo
                        tabView.setBackgroundColor(
                            ContextCompat.getColor(
                                this@EmpresaDetalhesActivity,
                                R.color.roxo
                            )
                        )
                    }

                    // Definindo a cor do texto da aba dependendo do tema
                    val textColor = if (isDarkMode) {
                        ContextCompat.getColor(
                            this@EmpresaDetalhesActivity,
                            R.color.white
                        ) // Branco no modo escuro
                    } else {
                        ContextCompat.getColor(
                            this@EmpresaDetalhesActivity,
                            R.color.roxo
                        ) // Roxo no modo claro
                    }

                    it.view.findViewById<TextView>(android.R.id.title)?.setTextColor(textColor)
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {
                tab?.let {
                    val tabView = it.view

                    // Definindo o fundo transparente quando a aba não está selecionada
                    tabView.setBackgroundColor(
                        ContextCompat.getColor(
                            this@EmpresaDetalhesActivity,
                            android.R.color.transparent
                        )
                    )

                    // Mudando a cor do texto para cinza, dependendo do tema
                    val textColor = if (isDarkMode) {
                        ContextCompat.getColor(
                            this@EmpresaDetalhesActivity,
                            R.color.cinza_escuro
                        ) // Cinza escuro no modo escuro
                    } else {
                        ContextCompat.getColor(
                            this@EmpresaDetalhesActivity,
                            R.color.cinza_escuro
                        ) // Cinza claro no modo claro
                    }
                    it.view.findViewById<TextView>(android.R.id.title)?.setTextColor(textColor)
                }
            }

            override fun onTabReselected(tab: TabLayout.Tab?) {
                // Esse método é chamado quando a aba é re-selecionada
                // Podemos manter as configurações de texto e indicador aqui, se necessário
            }
        })

        // Defina a cor do texto da aba inicial quando o layout for carregado
        tabLayout.getTabAt(viewPager.currentItem)?.let {
            val tabView = it.view

            // Definindo o fundo da aba inicial
            if (isDarkMode) {
                tabView.setBackgroundColor(
                    ContextCompat.getColor(
                        this,
                        android.R.color.transparent
                    )
                ) // Modo escuro
            } else {
                tabView.setBackgroundColor(ContextCompat.getColor(this, R.color.roxo)) // Modo claro
            }

            // Definindo a cor do texto da aba inicial
            val textColor = if (isDarkMode) {
                ContextCompat.getColor(this, R.color.white) // Branco no modo escuro
            } else {
                ContextCompat.getColor(this, R.color.roxo) // Roxo no modo claro
            }

            it.view.findViewById<TextView>(android.R.id.title)?.setTextColor(textColor)
        }

        // Defina a cor do traço da aba
        tabLayout.setSelectedTabIndicatorColor(ContextCompat.getColor(this, R.color.roxo)) // Roxo
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
