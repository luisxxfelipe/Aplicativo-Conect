package com.conect.aplicativoconect.view.ui.admin

import android.app.AlertDialog
import android.app.DatePickerDialog
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.conect.aplicativoconect.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.Calendar
import java.util.UUID

class AddBookingActivity : AppCompatActivity() {

    private lateinit var editTextName: EditText
    private lateinit var spinnerService: Spinner
    private lateinit var textViewSelectedDate: TextView
    private lateinit var textViewSelectedTime: TextView
    private lateinit var buttonSelectDate: Button
    private lateinit var buttonSelectTime: Button
    private lateinit var buttonSaveBooking: Button
    private var companyId: String? = null
    private val servicesMap = mutableMapOf<String, Double>() // Para associar serviço ao preço
    private var operatingHours: Pair<Int, Int>? =
        null // Horário de funcionamento (abertura e fechamento)
    private var availableTimes = mutableListOf<String>() // Horários disponíveis
    private lateinit var progressBarSaving: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_add_booking)

        editTextName = findViewById(R.id.editTextName)
        spinnerService = findViewById(R.id.spinnerService)
        textViewSelectedDate = findViewById(R.id.textViewSelectedDate)
        textViewSelectedTime = findViewById(R.id.textViewSelectedTime)
        buttonSelectDate = findViewById(R.id.buttonSelectDate)
        buttonSelectTime = findViewById(R.id.buttonSelectTime)
        buttonSaveBooking = findViewById(R.id.buttonSaveBooking)
        progressBarSaving = findViewById(R.id.progressBarSaving)

        // Definindo visibilidade inicial
        buttonSelectTime.visibility = View.GONE
        buttonSaveBooking.visibility = View.GONE
        textViewSelectedDate.visibility = View.GONE
        textViewSelectedTime.visibility = View.GONE

        fetchCompanyId() // Buscar o companyId e configurar o spinner

        buttonSelectDate.setOnClickListener { selectDate() }
        buttonSelectTime.setOnClickListener { showAvailableTimesDialog() }
        buttonSaveBooking.setOnClickListener { saveBooking() }
    }

    private fun fetchCompanyId() {
        val user = FirebaseAuth.getInstance().currentUser
        user?.uid?.let { userId ->
            FirebaseFirestore.getInstance().collection("business").whereEqualTo("ownerId", userId)
                .get()
                .addOnSuccessListener { documents ->
                    if (documents.isEmpty) {
                        Toast.makeText(this, "Nenhuma empresa encontrada.", Toast.LENGTH_SHORT)
                            .show()
                        return@addOnSuccessListener
                    }
                    companyId = documents.documents[0].id
                    loadServicesAndOperatingHours() // Carrega os serviços e horários
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(
                        this,
                        "Erro ao buscar companyId: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } ?: Toast.makeText(this, "Usuário não autenticado", Toast.LENGTH_SHORT).show()
    }

    private fun loadServicesAndOperatingHours() {
        companyId?.let { id ->
            FirebaseFirestore.getInstance().collection("business").document(id)
                .get()
                .addOnSuccessListener { document ->
                    val services =
                        document.get("services") as? List<Map<String, Any>> ?: emptyList()
                    services.forEach { service ->
                        val serviceName = service["serviceName"] as? String ?: ""
                        val price = (service["price"] as? Number)?.toDouble() ?: 0.0
                        servicesMap[serviceName] = price
                    }
                    setupServiceSpinner(servicesMap.keys.toList())

                    // Obter horários de funcionamento do negócio
                    val openingTime =
                        (document.getString("opening")?.split(":")?.get(0)?.toInt()) ?: 9
                    val closingTime =
                        (document.getString("closing")?.split(":")?.get(0)?.toInt()) ?: 18
                    operatingHours = Pair(openingTime, closingTime)
                }
                .addOnFailureListener {
                    Toast.makeText(
                        this,
                        "Erro ao carregar serviços e horários.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }

    private fun setupServiceSpinner(serviceNames: List<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, serviceNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerService.adapter = adapter
    }

    private fun selectDate() {
        val calendar = Calendar.getInstance()
        DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val selectedDate = String.format("%02d/%02d/%04d", dayOfMonth, month + 1, year)
                textViewSelectedDate.text = selectedDate
                textViewSelectedDate.visibility = View.VISIBLE

                // Exibir o botão de selecionar horário após selecionar a data
                buttonSelectTime.visibility = View.VISIBLE
                textViewSelectedTime.visibility = View.VISIBLE
                textViewSelectedTime.text = "Horário não selecionado" // Reseta o texto do horário

                fetchAvailableTimes(selectedDate) // Carregar horários disponíveis para a data
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        ).show()
    }

    private fun fetchAvailableTimes(date: String) {
        companyId?.let { id ->
            FirebaseFirestore.getInstance().collection("bookings")
                .whereEqualTo("companyId", id)
                .whereEqualTo("date", date)
                .get()
                .addOnSuccessListener { bookingsSnapshot ->
                    val bookedHours = bookingsSnapshot.documents.mapNotNull { it.getString("hour") }
                    generateAvailableTimes(bookedHours)
                }
                .addOnFailureListener {
                    Toast.makeText(this, "Erro ao buscar horários.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun generateAvailableTimes(bookedHours: List<String>) {
        val (opening, closing) = operatingHours ?: Pair(9, 18) // Horários padrão
        val allHours = (opening until closing).map { hour -> String.format("%02d:00", hour) }
        availableTimes = allHours.filterNot { bookedHours.contains(it) }.toMutableList()
    }

    private fun showAvailableTimesDialog() {
        if (availableTimes.isEmpty()) {
            Toast.makeText(
                this,
                "Selecione uma data para ver horários disponíveis.",
                Toast.LENGTH_SHORT
            ).show()
            return
        }

        val builder = AlertDialog.Builder(this)
        builder.setTitle("Selecione um Horário Disponível")
            .setItems(availableTimes.toTypedArray()) { _, which ->
                textViewSelectedTime.text = availableTimes[which] // Define o horário selecionado
                textViewSelectedTime.visibility = View.VISIBLE

                // Exibir o botão de salvar agendamento após selecionar o horário
                buttonSaveBooking.visibility = View.VISIBLE
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun saveBooking() {
        val name = editTextName.text.toString()
        val selectedServiceName = spinnerService.selectedItem.toString()
        val selectedDate = textViewSelectedDate.text.toString()
        val selectedHour = textViewSelectedTime.text.toString()

        if (name.isEmpty() || selectedDate == "Data não selecionada" || selectedHour == "Horário não selecionado") {
            Toast.makeText(this, "Preencha todos os campos!", Toast.LENGTH_SHORT).show()
            return
        }

        val servicePrice = servicesMap[selectedServiceName] ?: 0.0

        if (companyId != null) {
            uploadImageAndSaveBooking(
                name,
                selectedServiceName,
                selectedDate,
                selectedHour,
                servicePrice
            )
            progressBarSaving.visibility = View.VISIBLE
            buttonSaveBooking.isEnabled = false
        } else {
            Toast.makeText(this, "Company ID não encontrado.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun uploadImageAndSaveBooking(
        name: String,
        serviceName: String,
        date: String,
        hour: String,
        price: Double
    ) {
        val storageRef = FirebaseStorage.getInstance().reference
        val imageRef = storageRef.child("bookings_images/default_img.png")
        val drawableUri = Uri.parse("android.resource://${packageName}/drawable/default_img")

        imageRef.putFile(drawableUri)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    saveBookingToFirestore(name, serviceName, date, hour, price, uri.toString())
                }
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    this,
                    "Erro ao enviar imagem: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    private fun saveBookingToFirestore(
        name: String,
        serviceName: String,
        date: String,
        hour: String,
        price: Double,
        imageUrl: String
    ) {
        val bookingData = mapOf(
            "name" to name,
            "serviceName" to serviceName,
            "date" to date,
            "hour" to hour,
            "price" to price,
            "userId" to UUID.randomUUID().toString(),
            "companyId" to companyId!!,
            "status_cliente" to "confirmed",
            "status_adm" to "confirmed",
            "imageUrl" to imageUrl,
            "notified" to false
        )

        FirebaseFirestore.getInstance().collection("bookings")
            .add(bookingData)
            .addOnSuccessListener {
                Toast.makeText(this, "Agendamento salvo com sucesso!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { exception ->
                Toast.makeText(
                    this,
                    "Erro ao salvar agendamento: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }
}
