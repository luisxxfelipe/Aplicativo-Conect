package com.conect.aplicativoconect.view.ui.client

import android.app.DatePickerDialog
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.data.model.Service
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import java.text.SimpleDateFormat
import java.util.*

class SelecionarHorarioActivity : AppCompatActivity() {

    private lateinit var horariosRecyclerView: RecyclerView
    private lateinit var horariosAdapter: HorariosAdapter
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var companyId: String
    private lateinit var selectedService: Service
    private var selectedHour: Int? = null
    private var selectedDate: String? = null // Armazenar a data selecionada
    private lateinit var operatingHours: Map<String, Any> // Horários de funcionamento da empresa

    private lateinit var textViewSelectedDate: TextView // Exibe a data selecionada
    private lateinit var buttonPickDate: Button // Botão para abrir o seletor de datas

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_selecionar_horario)

        auth = FirebaseAuth.getInstance() // Inicializa o FirebaseAuth

        companyId = intent.getStringExtra("companyId") ?: ""
        selectedService = intent.getSerializableExtra("selectedService") as Service

        horariosRecyclerView = findViewById(R.id.horariosRecyclerView)
        horariosRecyclerView.layoutManager = LinearLayoutManager(this)

        textViewSelectedDate = findViewById(R.id.textViewSelectedDate)
        buttonPickDate = findViewById(R.id.buttonPickDate)

        firestore = FirebaseFirestore.getInstance()

        // Definir o comportamento do botão para selecionar a data
        buttonPickDate.setOnClickListener {
            openDatePicker()
        }

        // Buscar os horários de funcionamento da empresa
        fetchOperatingHours()

        val buttonAgendar = findViewById<Button>(R.id.buttonAgendar)
        buttonAgendar.setOnClickListener {
            selectedHour?.let { hour ->
                selectedDate?.let { date ->
                    val userId = auth.currentUser?.uid // Pega o UID do usuário autenticado
                    if (userId != null) {
                        // Buscar o nome do usuário no Firestore
                        fetchUserName(userId) { userName ->
                            val bookingData = mapOf(
                                "userId" to userId,
                                "companyId" to companyId,
                                "serviceName" to selectedService.name,
                                "hour" to hour,
                                "date" to date,
                                "name" to userName
                            )
                            firestore.collection("bookings").add(bookingData)
                                .addOnSuccessListener {
                                    Toast.makeText(this, "Agendamento realizado com sucesso!", Toast.LENGTH_SHORT).show()
                                    finish() // Voltar para a tela anterior
                                }
                                .addOnFailureListener { e ->
                                    Toast.makeText(this, "Erro ao agendar: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                        }
                    } else {
                        Toast.makeText(this, "Usuário não autenticado.", Toast.LENGTH_SHORT).show()
                    }
                } ?: run {
                    Toast.makeText(this, "Por favor, selecione uma data.", Toast.LENGTH_SHORT).show()
                }
            } ?: run {
                Toast.makeText(this, "Por favor, selecione um horário.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // Abre um DatePicker para o usuário selecionar a data
    private fun openDatePicker() {
        val calendar = Calendar.getInstance()
        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(year, month, dayOfMonth)
                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                selectedDate = dateFormat.format(selectedCalendar.time)
                textViewSelectedDate.text = selectedDate

                // Após selecionar a data, buscar os agendamentos existentes e horários disponíveis
                fetchExistingBookings(selectedDate)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        datePickerDialog.show()
    }

    // Busca os horários de funcionamento da empresa no Firestore
    private fun fetchOperatingHours() {
        firestore.collection("business").document(companyId).get()
            .addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    operatingHours = documentSnapshot.get("operatingHours") as Map<String, Any>
                } else {
                    Toast.makeText(this, "Horários de funcionamento não encontrados.", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao buscar horários de funcionamento: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchExistingBookings(date: String?) {
        if (date == null) return
        firestore.collection("bookings")
            .whereEqualTo("companyId", companyId)
            .whereEqualTo("date", date) // Busca os agendamentos para a data específica
            .get()
            .addOnSuccessListener { querySnapshot ->
                val horariosOcupados = getBookedHours(querySnapshot)
                val horariosDisponiveis = generateAvailableHours(horariosOcupados)
                horariosAdapter = HorariosAdapter(horariosDisponiveis) { selectedHour ->
                    this.selectedHour = selectedHour
                }
                horariosRecyclerView.adapter = horariosAdapter
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao buscar agendamentos: ${e.message}", Toast.LENGTH_SHORT).show()
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
        val opening = (operatingHours["opening"] as String).split(":")[0].toInt()
        val closing = (operatingHours["closing"] as String).split(":")[0].toInt()

        val allHours = (opening until closing).toList()
        return allHours.filter { hour ->
            bookedHours.none { bookedHour -> Math.abs(hour - bookedHour) < 1 } // Exclui horários já reservados
        }
    }

    private fun fetchUserName(userId: String, callback: (String) -> Unit) {
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { documentSnapshot ->
                val userName = documentSnapshot.getString("name") ?: "Nome não encontrado"
                callback(userName)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao buscar nome do usuário: ${e.message}", Toast.LENGTH_SHORT).show()
                callback("Nome não encontrado")
            }
    }
}
