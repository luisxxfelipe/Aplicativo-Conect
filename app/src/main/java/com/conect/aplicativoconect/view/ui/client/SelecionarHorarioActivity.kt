package com.conect.aplicativoconect.view.ui.client

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
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
    private lateinit var buttonAgendar: Button
    private lateinit var emptyStateImage: ImageView
    private lateinit var emptyStateText: TextView

    private lateinit var horariosAdapter: HorariosAdapter
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var companyId: String
    private lateinit var selectedService: Service
    private var selectedHour: String? = null
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
        buttonAgendar = findViewById(R.id.buttonAgendar)
        emptyStateImage = findViewById(R.id.emptyStateImage)
        emptyStateText = findViewById(R.id.emptyStateText)
        textViewSelectedDate = findViewById(R.id.textViewSelectedDate)

        horariosRecyclerView.layoutManager = LinearLayoutManager(this)

        // Esconde os componentes inicialmente
        horariosRecyclerView.visibility = View.GONE
        buttonAgendar.visibility = View.GONE

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
                                "price" to selectedService.price,
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

                // Exibe a lista e o botão, e esconde a imagem e texto
                emptyStateImage.visibility = View.GONE
                emptyStateText.visibility = View.GONE
                horariosRecyclerView.visibility = View.VISIBLE
                buttonAgendar.visibility = View.VISIBLE

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
            .whereEqualTo("date", date)
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


    private fun getBookedHours(querySnapshot: QuerySnapshot): List<String> {
        return querySnapshot.documents.mapNotNull { document ->
            document.getString("hour") // Extrai o horário como String (ex: "15:29")
        }
    }

    private fun generateAvailableHours(bookedHours: List<String>): List<String> {
        val openingTime = operatingHours["opening"] as? String ?: "09:00"
        val closingTime = operatingHours["closing"] as? String ?: "18:00"

        val openingHour = openingTime.split(":")[0].toInt()
        val closingHour = closingTime.split(":")[0].toInt()

        // Gera a lista de horários no formato "HH:mm"
        val allHours = (openingHour until closingHour).map { hour -> String.format("%02d:00", hour) }

        // Filtra os horários já reservados
        return allHours.filter { hour ->
            !bookedHours.contains(hour) // Exclui horários que já estão reservados
        }
    }


}
