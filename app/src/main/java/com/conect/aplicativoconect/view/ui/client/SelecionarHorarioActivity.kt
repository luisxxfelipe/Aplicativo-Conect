package com.conect.aplicativoconect.view.ui.client

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.data.model.Service
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class SelecionarHorarioActivity : AppCompatActivity() {

    private lateinit var buttonAdjustDate: Button
    private lateinit var buttonAgendar: Button
    private lateinit var emptyStateImage: ImageView
    private lateinit var emptyStateText: TextView
    private lateinit var buttonPickDate: Button

    private lateinit var summaryTitle: TextView
    private lateinit var summaryServiceName: TextView
    private lateinit var summaryDate: TextView
    private lateinit var summaryHour: TextView
    private lateinit var summaryPrice: TextView
    private lateinit var appointmentSummaryLayout: LinearLayout // Add this line

    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth
    private lateinit var companyId: String
    private lateinit var selectedService: Service
    private var selectedHour: String? = null
    private var selectedDate: String? = null
    private lateinit var operatingHours: Map<String, Any>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_selecionar_horario)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        companyId = intent.getStringExtra("companyId") ?: ""
        selectedService = intent.getSerializableExtra("selectedService") as Service

        emptyStateImage = findViewById(R.id.emptyStateImage)
        emptyStateText = findViewById(R.id.emptyStateText)
        buttonPickDate = findViewById(R.id.buttonPickDate)
        buttonAdjustDate = findViewById(R.id.buttonAdjustDate)
        buttonAgendar = findViewById(R.id.buttonAgendar)

        summaryTitle = findViewById(R.id.summaryTitle)
        summaryServiceName = findViewById(R.id.summaryServiceName)
        summaryDate = findViewById(R.id.summaryDate)
        summaryHour = findViewById(R.id.summaryHour)
        summaryPrice = findViewById(R.id.summaryPrice)
        appointmentSummaryLayout = findViewById(R.id.appointmentSummaryLayout) // Initialize here

        buttonAdjustDate.visibility = View.GONE
        buttonAgendar.visibility = View.GONE
        summaryTitle.visibility = View.GONE
        appointmentSummaryLayout.visibility = View.GONE // Ensure it's initially hidden

        buttonPickDate.setOnClickListener { openDatePicker() }
        buttonAdjustDate.setOnClickListener { openDatePicker() }
        buttonAgendar.setOnClickListener { scheduleAppointment() }

        fetchOperatingHours()
    }

    private fun openDatePicker() {
        val calendar = Calendar.getInstance()

        // Calcula a data máxima permitida (3 semanas a partir da data atual)
        val maxDateCalendar = Calendar.getInstance()
        maxDateCalendar.add(Calendar.WEEK_OF_YEAR, 3) // Adiciona 3 semanas

        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(year, month, dayOfMonth)

                // Verifica se a data selecionada está dentro do limite de 3 semanas
                if (selectedCalendar.after(maxDateCalendar)) {
                    Toast.makeText(this, "Não é possível agendar com mais de 3 semanas de antecedência.", Toast.LENGTH_SHORT).show()
                    return@DatePickerDialog
                }

                val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                selectedDate = dateFormat.format(selectedCalendar.time)
                buttonPickDate.text = "Ajustar Data"
                fetchExistingBookings(selectedDate!!)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        // Configura a data mínima para hoje
        datePickerDialog.datePicker.minDate = calendar.timeInMillis
        // Configura a data máxima para 3 semanas a partir de hoje
        datePickerDialog.datePicker.maxDate = maxDateCalendar.timeInMillis

        datePickerDialog.show()
    }


    private fun fetchOperatingHours() {
        firestore.collection("business").document(companyId).get()
            .addOnSuccessListener { documentSnapshot ->
                operatingHours = documentSnapshot.get("operatingHours") as Map<String, Any>
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao buscar horários: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private fun fetchExistingBookings(date: String) {
        firestore.collection("bookings")
            .whereEqualTo("companyId", companyId)
            .whereEqualTo("date", date)
            .get()
            .addOnSuccessListener { querySnapshot ->
                val bookedHours = getBookedHours(querySnapshot)
                val availableHours = generateAvailableHours(bookedHours)
                showHourSelectionDialog(availableHours)
            }
            .addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Erro ao buscar agendamentos: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun getBookedHours(querySnapshot: QuerySnapshot): List<String> {
        return querySnapshot.documents.mapNotNull { document -> document.getString("hour") }
    }

    private fun generateAvailableHours(bookedHours: List<String>): List<String> {
        val openingTime = operatingHours["opening"] as? String ?: "09:00"
        val closingTime = operatingHours["closing"] as? String ?: "18:00"

        val openingHour = openingTime.split(":")[0].toInt()
        val closingHour = closingTime.split(":")[0].toInt()

        val currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val isToday =
            selectedDate == SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

        val allHours =
            (openingHour until closingHour).map { hour -> String.format("%02d:00", hour) }

        return allHours.filter { hour ->
            (!isToday || hour.split(":")[0].toInt() > currentHour) && !bookedHours.contains(hour)
        }
    }

    private fun showHourSelectionDialog(availableHours: List<String>) {
        if (availableHours.isEmpty()) {
            Toast.makeText(this, "Nenhum horário disponível.", Toast.LENGTH_SHORT).show()
            return
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Selecione um horário")
        builder.setItems(availableHours.toTypedArray()) { _, which ->
            selectedHour = availableHours[which]
            buttonAdjustDate.visibility = View.VISIBLE
            buttonAgendar.visibility = View.VISIBLE
            displayAppointmentSummary()
        }
        builder.show()
    }

    private fun displayAppointmentSummary() {
        summaryTitle.visibility = View.VISIBLE
        appointmentSummaryLayout.visibility = View.VISIBLE

        summaryServiceName.text = "Serviço: ${selectedService.name}"
        summaryDate.text = "Data: $selectedDate"
        summaryHour.text = "Horário: $selectedHour"
        summaryPrice.text = "Preço: R$${selectedService.price}0"

        buttonAdjustDate.visibility = View.VISIBLE
        buttonAgendar.visibility = View.VISIBLE

        emptyStateImage.visibility = View.GONE
        emptyStateText.visibility = View.GONE
        buttonPickDate.visibility = View.GONE
    }

    private fun scheduleAppointment() {
        val userId = auth.currentUser?.uid ?: return
        val userRef = firestore.collection("users").document(userId)

        // Buscar o nome e a imagem do usuário
        userRef.get().addOnSuccessListener { userDoc ->
            val userName = userDoc.getString("name") ?: "Nome não encontrado"
            val userImageUrl = userDoc.getString("imageUrl")

            // Buscar o telefone do business
            val businessRef = firestore.collection("business").document(companyId)
            businessRef.get().addOnSuccessListener { businessDoc ->
                val businessPhone = businessDoc.getString("phone") ?: "Número não disponível"

                // Criar os dados do agendamento
                val bookingData = mapOf(
                    "userId" to userId,
                    "companyId" to companyId,
                    "serviceName" to selectedService.name,
                    "price" to selectedService.price,
                    "hour" to selectedHour,
                    "date" to selectedDate,
                    "name" to userName,
                    "userImageUrl" to userImageUrl,
                    "businessPhone" to businessPhone,  // Adiciona o telefone do business
                    "status_cliente" to "pending",
                    "status_adm" to "pending",
                    "notified" to false
                )

                // Salvar o agendamento no Firestore
                firestore.collection("bookings").add(bookingData)
                    .addOnSuccessListener {
                        Toast.makeText(this, "Agendamento realizado com sucesso!", Toast.LENGTH_SHORT).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Erro ao agendar: ${e.message}", Toast.LENGTH_SHORT).show()
                    }
            }.addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao buscar telefone do business: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(this, "Erro ao buscar dados do usuário: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

}
