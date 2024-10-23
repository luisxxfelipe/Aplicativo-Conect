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
import com.google.firebase.storage.FirebaseStorage
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
                    val companyImageUrl = document.getString("imageUrl") // URL da imagem salva no Firestore

                    // Preenche os detalhes da empresa
                    findViewById<TextView>(R.id.companyName).text = companyName
                    findViewById<TextView>(R.id.companyDescription).text = companyDescription

                    // Carregar a imagem usando Glide
                    val companyImageView = findViewById<ImageView>(R.id.companyImage)
                    loadCompanyImage(companyImageUrl, companyImageView)

                    // Pega os serviços como uma lista de Map (ou objetos)
                    val services = document.get("services") as? List<Map<String, Any>> ?: emptyList()

                    // Converte os serviços para uma lista de objetos Service
                    val serviceList = services.map { serviceMap ->
                        Service(
                            name = serviceMap["serviceName"] as String,
                            price = (serviceMap["price"] as Number).toDouble()
                        )
                    }

                    val servicesRecyclerView = findViewById<RecyclerView>(R.id.servicesRecyclerView)
                    val gridLayoutManager = GridLayoutManager(this, 2) // 2 colunas
                    servicesRecyclerView.layoutManager = gridLayoutManager
                    servicesRecyclerView.adapter = ServicesAdapter(serviceList, { selectedService ->
                        this.selectedService = selectedService
                    }, { serviceToBook ->
                        val intent: Intent = Intent(this, SelecionarHorarioActivity::class.java)
                        intent.putExtra("selectedService", serviceToBook as Serializable)
                        intent.putExtra("companyId", companyId)
                        startActivity(intent)
                    })

                } else {
                    finish()
                }
            }
            .addOnFailureListener { e ->
                e.printStackTrace()
                finish()
            }
    }

    private fun loadCompanyImage(imageUrl: String?, imageView: ImageView) {
        if (!imageUrl.isNullOrEmpty()) {
            Log.d("EmpresaDetalhesActivity", "Loading image from URL: $imageUrl")
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.about_business) // Placeholder enquanto carrega
                .error(R.drawable.about_business) // Imagem de erro, se houver falha no carregamento
                .diskCacheStrategy(DiskCacheStrategy.ALL) // Cache da imagem
                .into(imageView)
        } else {
            Log.e("EmpresaDetalhesActivity", "Image URL is null or empty")
            imageView.setImageResource(R.drawable.about_business) // Imagem padrão caso não haja URL
        }
    }
}
