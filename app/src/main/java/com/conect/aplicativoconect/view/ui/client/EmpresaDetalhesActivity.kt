package com.conect.aplicativoconect.view.ui.client

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.data.model.Service
import com.google.firebase.firestore.FirebaseFirestore
import java.io.Serializable

class EmpresaDetalhesActivity : AppCompatActivity() {

    private lateinit var companyId: String
    private lateinit var selectedService: Service

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_about_company)

        companyId = intent.getStringExtra("companyId") ?: ""

        if (companyId.isNotEmpty()) {
            fetchCompanyDetails(companyId)
        } else {
            finish()
        }
    }

    private fun fetchCompanyDetails(companyId: String) {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("business").document(companyId).get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val companyName = document.getString("name") ?: ""
                    val companyDescription = document.getString("description") ?: ""
                    val companyImageUrl = document.getString("imageUrl")
                    val profileImageUrl = document.getString("imageUrl")

                    // Tratamento correto para o campo 'operatingHours'
                    val operatingHoursMap = document.get("operatingHours") as? Map<String, String>
                    val opening = operatingHoursMap?.get("opening") ?: "N/A"
                    val closing = operatingHoursMap?.get("closing") ?: "N/A"
                    val operatingHours = "Horário: $opening - $closing"

                    val category = document.getString("serviceType") ?: "Categoria não informada"

                    // Preenche os TextViews com os dados da empresa
                    findViewById<TextView>(R.id.companyName).text = companyName
                    findViewById<TextView>(R.id.companyDescription).text = companyDescription
                    findViewById<TextView>(R.id.companyOperatingHours).text = operatingHours
                    findViewById<TextView>(R.id.companyCategory).text = category

                    // Carregar as imagens usando Glide
                    val companyImageView = findViewById<ImageView>(R.id.companyImage)
                    val profileImageView = findViewById<ImageView>(R.id.companyProfileImage)

                    loadImage(companyImageUrl, companyImageView)
                    loadImage(profileImageUrl, profileImageView)

                    // Configurar RecyclerView dos serviços
                    val services = document.get("services") as? List<Map<String, Any>> ?: emptyList()
                    val serviceList = services.map { serviceMap ->
                        Service(
                            name = serviceMap["serviceName"] as String,
                            price = (serviceMap["price"] as Number).toDouble()
                        )
                    }

                    val servicesRecyclerView = findViewById<RecyclerView>(R.id.servicesRecyclerView)
                    val gridLayoutManager = GridLayoutManager(this, 2)
                    servicesRecyclerView.layoutManager = gridLayoutManager

                    servicesRecyclerView.adapter = ServicesAdapter(serviceList, { selectedService ->
                        this.selectedService = selectedService
                    }, { serviceToBook ->
                        val intent = Intent(this, SelecionarHorarioActivity::class.java)
                        intent.putExtra("selectedService", serviceToBook as Serializable)
                        intent.putExtra("companyId", companyId)
                        startActivity(intent)
                    })

                } else {
                    finish() // Se o documento não existir
                }
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                finish() // Em caso de erro
            }
    }

    private fun loadImage(imageUrl: String?, imageView: ImageView) {
        if (!imageUrl.isNullOrEmpty()) {
            Log.d("EmpresaDetalhesActivity", "Carregando imagem: $imageUrl")
            Glide.with(this)
                .load(imageUrl)
                .thumbnail(0.1f) // Carrega uma miniatura primeiro
                .override(300, 300) // Redimensiona para evitar bitmaps grandes
                .centerCrop()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(imageView)
        } else {
            Log.e("EmpresaDetalhesActivity", "URL de imagem nula ou vazia")
            imageView.setImageResource(R.drawable.foto_perfil_generica)
        }
    }


}
