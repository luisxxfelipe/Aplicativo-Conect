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
                        // Buscar o nome e a imagem do usuário no Firestore
                        fetchUserDetails(userId) { userName, userImageUrl ->
                            val bookingData = mapOf(
                                "userId" to userId,
                                "companyId" to companyId,
                                "serviceName" to selectedService.name,
                                "hour" to hour,
                                "date" to date,
                                "name" to userName,
                                "userImageUrl" to userImageUrl, // Salvar a URL da imagem de perfil
                                "status_cliente" to "pending",
                                "status_adm" to "pending"
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

    private fun fetchUserDetails(userId: String, callback: (String, String?) -> Unit) {
        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { documentSnapshot ->
                val userName = documentSnapshot.getString("name") ?: "Nome não encontrado"
                val userImageUrl = documentSnapshot.getString("imageUrl") // Busca a URL da imagem
                callback(userName, userImageUrl)
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao buscar detalhes do usuário: ${e.message}", Toast.LENGTH_SHORT).show()
                callback("Nome não encontrado", null)
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
                val formattedDate = dateFormat.format(selectedCalendar.time)

                // Atualiza o TextView e a variável de data selecionada
                selectedDate = formattedDate
                textViewSelectedDate.text = formattedDate

                // Usa uma cópia segura da data para evitar o problema de smart cast
                fetchExistingBookings(formattedDate)
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

    private fun fetchExistingBookings(date: String) {
        firestore.collection("bookings")
            .whereEqualTo("companyId", companyId)
            .whereEqualTo("date", date) // Busca os agendamentos para a data específica
            .get()
            .addOnSuccessListener { querySnapshot ->
                val horariosOcupados = getBookedHours(querySnapshot)

                // Gerar horários disponíveis excluindo os já ocupados
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
        return querySnapshot.documents.mapNotNull { document ->
            document.getLong("hour")?.toInt() // Extrai a hora reservada
        }
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
