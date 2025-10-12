package com.conect.aplicativoconect.ui.activities

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
import androidx.core.content.ContextCompat
import com.conect.aplicativoconect.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.conect.aplicativoconect.utils.Validator
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
    private var companyName: String? = null
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
            FirebaseFirestore.getInstance().collection("business").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document.exists()) {
                        companyId = document.id
                        companyName = document.getString("name") // Obter o nome da empresa
                        loadServicesAndOperatingHours() // Carrega os serviços e horários
                    } else {
                        Toast.makeText(this, "Nenhuma empresa encontrada.", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(
                        this,
                        "Erro ao buscar empresa: ${exception.message}",
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
                    // Carregar serviços
                    val services =
                        document.get("services") as? List<Map<String, Any>> ?: emptyList()
                    if (services.isNotEmpty()) {
                        servicesMap.clear() // Certifique-se de limpar o mapa antes de adicionar novos itens
                        services.forEach { service ->
                            val serviceName =
                                service["serviceName"] as? String ?: "Serviço Desconhecido"
                            val price = (service["price"] as? Number)?.toDouble() ?: 0.0
                            servicesMap[serviceName] = price
                        }
                        setupServiceSpinner(servicesMap.keys.toList()) // Configura o spinner com os serviços
                    }

                    // Carregar horários de funcionamento
                    val operatingHoursMap = document.get("operatingHours") as? Map<String, String>
                    val openingTime =
                        operatingHoursMap?.get("opening")?.split(":")?.get(0)?.toInt() ?: 9
                    val closingTime =
                        operatingHoursMap?.get("closing")?.split(":")?.get(0)?.toInt() ?: 18
                    operatingHours = Pair(openingTime, closingTime)
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(
                        this,
                        "Erro ao carregar dados: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } ?: Toast.makeText(this, "ID da empresa não encontrado.", Toast.LENGTH_SHORT).show()
    }


    private fun setupServiceSpinner(serviceNames: List<String>) {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, serviceNames)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerService.adapter = adapter
    }

    private fun selectDate() {
        val calendar = Calendar.getInstance()

        // Cria o DatePickerDialog
        val datePickerDialog = DatePickerDialog(
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
        )

        // Aqui, você pode personalizar os botões após o diálogo ser mostrado
        datePickerDialog.setOnShowListener {
            val buttonOk = datePickerDialog.getButton(DatePickerDialog.BUTTON_POSITIVE)
            val buttonCancel = datePickerDialog.getButton(DatePickerDialog.BUTTON_NEGATIVE)

            // Defina a cor do texto do botão "OK" para roxo
            buttonOk.setTextColor(ContextCompat.getColor(this, R.color.roxo))

            // Defina a cor do texto do botão "Cancelar" (opcional)
            buttonCancel.setTextColor(
                ContextCompat.getColor(
                    this,
                    R.color.red
                )
            ) // Exemplo: vermelho
        }

        // Exibe o DatePickerDialog
        datePickerDialog.show()
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

        // Criação e exibição do diálogo
        val dialog = builder.create()
        dialog.show()

        // Acessando o botão "Cancelar" e alterando a cor do texto
        val negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
        negativeButton.setTextColor(
            ContextCompat.getColor(
                this,
                R.color.roxo
            )
        ) // Cor roxa para o texto
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
        // Calculando o timestamp a partir da data e hora selecionadas
        val dateTimeString = "$date $hour"
        val timestamp = Validator.DATE_TIME_FORMAT.parse(dateTimeString)?.time ?: System.currentTimeMillis() // ✅ OTIMIZADO: Formatador central

        val bookingData = mapOf(
            "name" to name,
            "serviceName" to serviceName,
            "date" to date,
            "hour" to hour,
            "timestamp" to timestamp, // Adicionando o campo timestamp
            "price" to price,
            "userId" to UUID.randomUUID().toString(),
            "companyId" to companyId!!,
            "companyName" to (companyName
                ?: "Nome não disponível"), // Adicionando o nome da empresa
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
