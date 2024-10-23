package com.conect.aplicativoconect.view.ui.client

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.data.model.Service
import com.google.firebase.firestore.FirebaseFirestore

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

                    // Pega os serviços como uma lista de Map (ou objetos) em vez de Strings
                    val services = document.get("services") as? List<Map<String, Any>> ?: emptyList()

                    // Preenche os detalhes da empresa
                    findViewById<TextView>(R.id.companyName).text = companyName
                    findViewById<TextView>(R.id.companyDescription).text = companyDescription

                    // Converte os serviços para uma lista de objetos Service
                    val serviceList = services.map { serviceMap ->
                        Service(
                            name = serviceMap["serviceName"] as String,
                            price = (serviceMap["price"] as Number).toDouble()
                        )
                    }

                    // Configura o RecyclerView com os serviços da empresa
                    val servicesRecyclerView = findViewById<RecyclerView>(R.id.servicesRecyclerView)
                    servicesRecyclerView.layoutManager = LinearLayoutManager(this)
                    servicesRecyclerView.adapter = ServicesAdapter(serviceList, { selectedService ->
                        this.selectedService = selectedService // Armazena o serviço selecionado
                    }, { serviceToBook ->
                        // Redireciona para SelecionarHorarioActivity ao clicar no botão "Agendar"
                        val intent: Intent = Intent(this, SelecionarHorarioActivity::class.java)
                        intent.putExtra("selectedService", serviceToBook)
                        intent.putExtra("companyId", companyId) // Passa o ID da empresa para o agendamento
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
}
