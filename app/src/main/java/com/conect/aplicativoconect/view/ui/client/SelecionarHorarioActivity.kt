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

    private var workingDays: List<String> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_selecionar_horario)

        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        companyId = intent.getStringExtra("companyId") ?: ""
        selectedService = intent.getSerializableExtra("selectedService") as Service

        // Fetch operating hours and average duration
        fetchOperatingHoursAndAverageDuration()

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
        appointmentSummaryLayout = findViewById(R.id.appointmentSummaryLayout)

        buttonAdjustDate.visibility = View.GONE
        buttonAgendar.visibility = View.GONE
        summaryTitle.visibility = View.GONE
        appointmentSummaryLayout.visibility = View.GONE

        buttonPickDate.setOnClickListener { openDatePicker() }
        buttonAdjustDate.setOnClickListener { openDatePicker() }
        buttonAgendar.setOnClickListener { scheduleAppointment() }
    }

    private fun openDatePicker() {
        val calendar = Calendar.getInstance()

        // Calcula a data máxima permitida (3 semanas a partir da data atual)
        val maxDateCalendar = Calendar.getInstance()
        maxDateCalendar.add(Calendar.WEEK_OF_YEAR, 3)

        val datePickerDialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedCalendar = Calendar.getInstance()
                selectedCalendar.set(year, month, dayOfMonth)

                // Obtenha o nome do dia da semana no idioma local
                val dayOfWeek = selectedCalendar.getDisplayName(
                    Calendar.DAY_OF_WEEK,
                    Calendar.LONG,
                    Locale.getDefault()
                )

                if (workingDays.contains(dayOfWeek)) { // Verifica se o dia está nos dias trabalhados
                    val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
                    selectedDate = dateFormat.format(selectedCalendar.time)
                    buttonPickDate.text = "Ajustar Data"
                    fetchExistingBookings(selectedDate!!)
                } else {
                    Toast.makeText(this, "A empresa não trabalha neste dia.", Toast.LENGTH_SHORT).show()
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        datePickerDialog.datePicker.minDate = calendar.timeInMillis
        datePickerDialog.datePicker.maxDate = maxDateCalendar.timeInMillis
        datePickerDialog.show()
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

    private fun fetchOperatingHoursAndAverageDuration() {
        firestore.collection("business").document(companyId).get()
            .addOnSuccessListener { documentSnapshot ->
                operatingHours = documentSnapshot.get("operatingHours") as Map<String, Any>

                val daysInEnglish = documentSnapshot.get("operatingHours.days") as? List<String> ?: emptyList()

                // Converte os dias para o idioma local, se necessário
                workingDays = convertDaysToLocale(daysInEnglish)

                val averageDuration = documentSnapshot.getDouble("averageDuration") ?: 30.0
                selectedService.duration = averageDuration.toInt()
            }
    }

    private fun convertDaysToLocale(daysInEnglish: List<String>): List<String> {
        val currentLanguage = Locale.getDefault().language // Verifica o idioma local
        if (currentLanguage == "pt") { // Se o idioma local for português
            val daysMap = mapOf(
                "Monday" to "segunda-feira",
                "Tuesday" to "terça-feira",
                "Wednesday" to "quarta-feira",
                "Thursday" to "quinta-feira",
                "Friday" to "sexta-feira",
                "Saturday" to "sábado",
                "Sunday" to "domingo"
            )
            return daysInEnglish.map { daysMap[it] ?: it } // Converte para português
        }
        return daysInEnglish // Retorna em inglês se o idioma local não for português
    }


    private fun generateAvailableHours(bookedHours: List<String>): List<String> {
        val openingTime = operatingHours["opening"] as? String ?: "06:00"
        val closingTime = operatingHours["closing"] as? String ?: "18:00"
        val lunchStart = "12:00"
        val lunchEnd = "13:00"
        val serviceDuration = selectedService.duration ?: 30 // Utilizar averageDuration

        val availableSlots = mutableListOf<String>()

        // Obter data atual e selecionada
        val dateFormat = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        val currentDateTime = Calendar.getInstance()
        val currentDate = dateFormat.format(currentDateTime.time)
        val selectedCalendar = Calendar.getInstance()
        selectedCalendar.time = dateFormat.parse(selectedDate!!)
        val selectedDayOfWeek = selectedCalendar.getDisplayName(
            Calendar.DAY_OF_WEEK,
            Calendar.LONG,
            Locale.getDefault()
        )

        // Validar se o dia selecionado está nos dias trabalhados
        if (!workingDays.contains(selectedDayOfWeek)) {
            Toast.makeText(this, "A empresa não trabalha no dia selecionado.", Toast.LENGTH_SHORT)
                .show()
            return availableSlots // Retorna vazio
        }

        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val currentTime = timeFormat.format(currentDateTime.time)

        // Filtrar horários com base na data selecionada
        if (selectedDate == currentDate) {
            // Para hoje, excluir horários passados
            availableSlots.addAll(
                generateTimeSlotsDynamic(
                    openingTime,
                    lunchStart,
                    serviceDuration,
                    bookedHours,
                    currentTime
                )
            )
            availableSlots.addAll(
                generateTimeSlotsDynamic(
                    lunchEnd,
                    closingTime,
                    serviceDuration,
                    bookedHours,
                    currentTime
                )
            )
        } else {
            // Para dias futuros, exibir todos os horários disponíveis
            availableSlots.addAll(
                generateTimeSlotsDynamic(
                    openingTime,
                    lunchStart,
                    serviceDuration,
                    bookedHours,
                    null
                )
            )
            availableSlots.addAll(
                generateTimeSlotsDynamic(lunchEnd, closingTime, serviceDuration, bookedHours, null)
            )
        }

        return availableSlots
    }

    private fun generateTimeSlotsDynamic(
        start: String,
        end: String,
        duration: Int,
        bookedHours: List<String>,
        currentTime: String?
    ): List<String> {
        val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
        val slots = mutableListOf<String>()

        var current = timeFormat.parse(start)
        val endTime = timeFormat.parse(end)
        val now = if (currentTime != null) timeFormat.parse(currentTime) else null

        while (current.before(endTime)) {
            val formattedSlot = timeFormat.format(current)

            // Verifica se o bloco atual está livre e, se necessário, se está no futuro
            if ((now == null || current.after(now)) && !isSlotOverlapping(
                    formattedSlot,
                    duration,
                    bookedHours,
                    timeFormat
                )
            ) {
                slots.add(formattedSlot)
            }

            // Incrementa pelo tempo de duração do serviço
            val calendar = Calendar.getInstance()
            calendar.time = current
            calendar.add(Calendar.MINUTE, duration)
            current = calendar.time
        }

        return slots
    }


    // Função para verificar sobreposição de horários
    private fun isSlotOverlapping(
        slot: String,
        duration: Int,
        bookedHours: List<String>,
        timeFormat: SimpleDateFormat
    ): Boolean {
        val slotStart = timeFormat.parse(slot)
        val calendar = Calendar.getInstance()
        calendar.time = slotStart
        calendar.add(Calendar.MINUTE, duration)
        val slotEnd = calendar.time

        for (booked in bookedHours) {
            val bookedStart = timeFormat.parse(booked)
            calendar.time = bookedStart
            calendar.add(Calendar.MINUTE, duration)
            val bookedEnd = calendar.time

            if (slotStart.before(bookedEnd) && slotEnd.after(bookedStart)) {
                return true
            }
        }

        return false
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

            // Buscar o telefone e o nome do business
            val businessRef = firestore.collection("business").document(companyId)
            businessRef.get().addOnSuccessListener { businessDoc ->
                val businessPhone = businessDoc.getString("phone") ?: "Número não disponível"
                val companyName = businessDoc.getString("name") ?: "Nome da empresa não disponível"

                // Calcular o timestamp com base na data e hora selecionadas
                val dateTimeString = "$selectedDate $selectedHour" // Combina data e hora
                val dateTimeFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
                val timestamp =
                    dateTimeFormat.parse(dateTimeString)?.time ?: System.currentTimeMillis()

                // Criar os dados do agendamento
                val bookingData = mapOf(
                    "userId" to userId,
                    "companyId" to companyId,
                    "companyName" to companyName, // Adiciona o nome da empresa
                    "serviceName" to selectedService.name,
                    "price" to selectedService.price,
                    "hour" to selectedHour,
                    "date" to selectedDate,
                    "timestamp" to timestamp, // Adiciona o campo timestamp
                    "name" to userName,
                    "userImageUrl" to userImageUrl,
                    "businessPhone" to businessPhone, // Adiciona o telefone do business
                    "status_cliente" to "pending",
                    "status_adm" to "pending",
                    "notified" to false
                )

                // Salvar o agendamento no Firestore
                firestore.collection("bookings").add(bookingData)
                    .addOnSuccessListener {
                        Toast.makeText(
                            this,
                            "Agendamento realizado com sucesso!",
                            Toast.LENGTH_SHORT
                        ).show()
                        finish()
                    }
                    .addOnFailureListener { e ->
                        Toast.makeText(this, "Erro ao agendar: ${e.message}", Toast.LENGTH_SHORT)
                            .show()
                    }
            }.addOnFailureListener { e ->
                Toast.makeText(
                    this,
                    "Erro ao buscar dados do business: ${e.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(
                this,
                "Erro ao buscar dados do usuário: ${e.message}",
                Toast.LENGTH_SHORT
            ).show()
        }
    }
}
