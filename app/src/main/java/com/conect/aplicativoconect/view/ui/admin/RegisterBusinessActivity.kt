package com.conect.aplicativoconect.view.ui.admin

import android.content.Intent
import android.os.Bundle
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.data.model.Business
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterBusinessActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var selectedServiceType: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_business)

        // Inicializar Firebase Auth e Firestore
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Referências aos componentes
        val businessNameInput = findViewById<TextInputEditText>(R.id.businessNameInput)
        val serviceTypeSpinner = findViewById<Spinner>(R.id.serviceTypeSpinner) // Usar Spinner
        val addressInput = findViewById<TextInputEditText>(R.id.addressInput)
        val operatingHoursInput = findViewById<TextInputEditText>(R.id.operatingHoursInput)
        val phoneInput = findViewById<TextInputEditText>(R.id.phoneInput)
        val registerBusinessButton = findViewById<MaterialButton>(R.id.registerBusinessButton)

        // Configurar o Spinner com opções de serviços
        val serviceTypes = listOf("Cabeleireiro", "Manicure", "Barbeiro", "Estética", "Massagem")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, serviceTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        serviceTypeSpinner.adapter = adapter

        serviceTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: android.view.View?, position: Int, id: Long) {
                selectedServiceType = serviceTypes[position] // Captura o tipo de serviço selecionado
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedServiceType = "" // Define como vazio se nada for selecionado
            }
        }

        registerBusinessButton.setOnClickListener {
            val businessName = businessNameInput.text.toString()
            val address = addressInput.text.toString()
            val operatingHours = operatingHoursInput.text.toString()
            val phone = phoneInput.text.toString()

            // Verificar se os campos estão preenchidos
            if (businessName.isEmpty() || selectedServiceType.isEmpty() || address.isEmpty() || operatingHours.isEmpty() || phone.isEmpty()) {
                Toast.makeText(this, "Por favor, preencha todos os campos", Toast.LENGTH_SHORT).show()
            } else {
                // Salvar empresa no Firestore
                val currentUserUid = auth.currentUser?.uid ?: return@setOnClickListener
                val business = Business(
                    uid = currentUserUid,
                    name = businessName,
                    serviceType = selectedServiceType, // Usar o tipo de serviço selecionado
                    address = address,
                    operatingHours = operatingHours,
                    phone = phone
                )

                firestore.collection("business").document(currentUserUid)
                    .set(business)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Empresa cadastrada com sucesso", Toast.LENGTH_SHORT).show()
                        // Redirecionar para a tela AdminHomeActivity após o sucesso do cadastro
                        startActivity(Intent(this, AdminHomeActivity::class.java))
                        finish() // Finaliza a Activity
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Falha ao cadastrar empresa", Toast.LENGTH_SHORT).show()
                    }
            }
        }
    }
}
