package com.conect.aplicativoconect.view.ui.client

import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.data.model.Service
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot

class SelecionarHorarioActivity : AppCompatActivity() {

    private lateinit var horariosRecyclerView: RecyclerView
    private lateinit var horariosAdapter: HorariosAdapter
    private lateinit var firestore: FirebaseFirestore
    private lateinit var companyId: String
    private lateinit var selectedService: Service
    private var selectedHour: Int? = null // Variável para armazenar o horário selecionado

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_selecionar_horario)

        companyId = intent.getStringExtra("companyId") ?: ""
        selectedService = intent.getSerializableExtra("selectedService") as Service

        horariosRecyclerView = findViewById(R.id.horariosRecyclerView)
        horariosRecyclerView.layoutManager = LinearLayoutManager(this)

        firestore = FirebaseFirestore.getInstance()

        fetchExistingBookings()

        val buttonAgendar = findViewById<Button>(R.id.buttonAgendar)
        buttonAgendar.setOnClickListener {
            selectedHour?.let { hour ->
                // Aqui você pode processar o agendamento
                val bookingData = mapOf(
                    "companyId" to companyId,
                    "serviceName" to selectedService.name,
                    "hour" to hour
                )
                firestore.collection("bookings").add(bookingData)
                    .addOnSuccessListener {
                        // Agendamento realizado com sucesso
                        Toast.makeText(this, "Agendamento realizado com sucesso!", Toast.LENGTH_SHORT).show()
                        finish() // Voltar para a tela anterior
                    }
                    .addOnFailureListener { e ->
                        // Lidar com falha no agendamento
                        Toast.makeText(this, "Erro ao agendar: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            } ?: run {
                // Mensagem de erro se nenhum horário foi selecionado
                Toast.makeText(this, "Por favor, selecione um horário.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun fetchExistingBookings() {
        firestore.collection("bookings")
            .whereEqualTo("companyId", companyId)
            .whereEqualTo("serviceName", selectedService.name)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val horariosOcupados = getBookedHours(querySnapshot)
                val horariosDisponiveis = generateAvailableHours(horariosOcupados)
                horariosAdapter = HorariosAdapter(horariosDisponiveis) { selectedHour ->
                    // Atualiza o horário selecionado
                    this.selectedHour = selectedHour
                }
                horariosRecyclerView.adapter = horariosAdapter
            }
    }

    private fun getBookedHours(querySnapshot: QuerySnapshot): List<Int> {
        val bookedHours = mutableListOf<Int>()
        for (document in querySnapshot) {
            val bookedHour = document.getLong("hour")?.toInt() ?: continue
            bookedHours.add(bookedHour)
        }
        return bookedHours
    }

    private fun generateAvailableHours(bookedHours: List<Int>): List<Int> {
        val allHours = (9..17).toList() // Horários de 9h a 17h
        return allHours.filter { hour ->
            bookedHours.none { bookedHour -> Math.abs(hour - bookedHour) < 1 } // Exclui horários com menos de 1h de diferença
        }
    }
}
