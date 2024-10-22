package com.conect.aplicativoconect.view.ui.client

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.conect.aplicativoconect.R
import com.google.firebase.firestore.FirebaseFirestore

class EmpresaDetalhesActivity : AppCompatActivity() {

    private lateinit var companyId: String
    private lateinit var selectedService: String

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
                    val services = document.get("services") as? List<String> ?: emptyList()

                    findViewById<TextView>(R.id.companyName).text = companyName
                    findViewById<TextView>(R.id.companyDescription).text = companyDescription

                    val servicesRecyclerView = findViewById<RecyclerView>(R.id.servicesRecyclerView)
                    servicesRecyclerView.layoutManager = LinearLayoutManager(this)
                    servicesRecyclerView.adapter = ServicesAdapter(services, { selectedService ->
                        this.selectedService = selectedService // Armazena o serviço selecionado
                    }, { serviceToBook ->
                        // Redireciona para SelecionarHorarioActivity ao clicar no botão "Agendar"
                        val intent = Intent(this, SelecionarHorarioActivity::class.java)
                        intent.putExtra("selectedService", serviceToBook)
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