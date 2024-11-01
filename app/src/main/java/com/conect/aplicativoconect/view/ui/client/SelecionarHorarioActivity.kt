package com.conect.aplicativoconect.view.ui.client

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.data.model.Service
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import java.text.SimpleDateFormat
import java.util.*

class SelecionarHorarioActivity : AppCompatActivity() {

    private lateinit var buttonAgendar: Button
    private lateinit var emptyStateImage: ImageView
    private lateinit var emptyStateText: TextView
    private lateinit var textViewSelectedDate: TextView
    private lateinit var buttonPickDate: Button

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
        textViewSelectedDate = findViewById(R.id.textViewSelectedDate)
        buttonPickDate = findViewById(R.id.buttonPickDate)
        buttonAgendar = findViewById(R.id.buttonAgendar)

        // Configurações iniciais
        textViewSelectedDate.visibility = View.GONE
        buttonAgendar.visibility = View.GONE

        buttonPickDate.setOnClickListener { openDatePicker() }
        buttonAgendar.setOnClickListener { scheduleAppointment() }

        fetchOperatingHours()
    }

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
                textViewSelectedDate.visibility = View.VISIBLE
                emptyStateImage.visibility = View.GONE
                emptyStateText.visibility = View.GONE

                fetchExistingBookings(selectedDate!!)
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )
        // Define a data mínima para o dia atual
        datePickerDialog.datePicker.minDate = calendar.timeInMillis
        datePickerDialog.show()
    }


    private fun fetchOperatingHours() {
        firestore.collection("business").document(companyId).get()
            .addOnSuccessListener { documentSnapshot ->
                operatingHours = documentSnapshot.get("operatingHours") as Map<String, Any>
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao buscar horários: ${e.message}", Toast.LENGTH_SHORT).show()
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
                Toast.makeText(this, "Erro ao buscar agendamentos: ${e.message}", Toast.LENGTH_SHORT).show()
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
        val isToday = selectedDate == SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date())

        val allHours = (openingHour until closingHour).map { hour -> String.format("%02d:00", hour) }

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
            buttonAgendar.visibility = View.VISIBLE
        }
        builder.show()
    }

    private fun scheduleAppointment() {
        val userId = auth.currentUser?.uid ?: return
        val userRef = firestore.collection("users").document(userId)

        userRef.get().addOnSuccessListener { userDoc ->
            val userName = userDoc.getString("name") ?: "Nome não encontrado"
            val userImageUrl = userDoc.getString("imageUrl")

            val bookingData = mapOf(
                "userId" to userId,
                "companyId" to companyId,
                "serviceName" to selectedService.name,
                "price" to selectedService.price,
                "hour" to selectedHour,
                "date" to selectedDate,
                "name" to userName,
                "userImageUrl" to userImageUrl,
                "status_cliente" to "pending",
                "status_adm" to "pending"
            )

            firestore.collection("bookings").add(bookingData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Agendamento realizado com sucesso!", Toast.LENGTH_SHORT).show()
                    finish()
                }
                .addOnFailureListener { e ->
                    Toast.makeText(this, "Erro ao agendar: ${e.message}", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
