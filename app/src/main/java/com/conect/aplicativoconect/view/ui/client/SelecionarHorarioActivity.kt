package com.conect.aplicativoconect.view.ui.client

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.conect.aplicativoconect.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SelecionarHorarioActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_select_time) // Certifique-se de usar o layout correto

        val selectedService = intent.getStringExtra("selectedService") ?: ""

        findViewById<TextView>(R.id.selectedService).text = selectedService

        // Buscar horários disponíveis para o serviço selecionado
        fetchAvailableTimes(selectedService)
    }

    private fun fetchAvailableTimes(service: String) {
        // Exemplo de horários disponíveis
        val availableTimes = listOf("10:00 AM", "11:00 AM", "02:00 PM", "03:00 PM")

        val scheduleRecyclerView = findViewById<RecyclerView>(R.id.scheduleRecyclerView)
        scheduleRecyclerView.layoutManager = LinearLayoutManager(this)
        scheduleRecyclerView.adapter = TimesAdapter(availableTimes) { selectedTime ->
            // Habilitar o botão "Agendar Agora" quando um horário for selecionado
            findViewById<Button>(R.id.bookNowButton).isEnabled = true

            findViewById<Button>(R.id.bookNowButton).setOnClickListener {
                // Realizar o agendamento
                bookAppointment(service, selectedTime)
            }
        }
    }

    private fun bookAppointment(service: String, time: String) {
        // Lógica para gravar o agendamento no Firestore
        val firestore = FirebaseFirestore.getInstance()
        val booking = hashMapOf(
            "service" to service,
            "time" to time,
            "userEmail" to FirebaseAuth.getInstance().currentUser?.email
        )

        firestore.collection("bookings").add(booking)
            .addOnSuccessListener {
                // Notificar o sucesso do agendamento
                Toast.makeText(this, "Agendamento realizado com sucesso!", Toast.LENGTH_LONG).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao realizar o agendamento.", Toast.LENGTH_LONG).show()
            }
    }
}
