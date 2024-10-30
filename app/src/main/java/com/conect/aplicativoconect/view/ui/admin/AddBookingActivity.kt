package com.conect.aplicativoconect.view.ui.admin

import android.net.Uri
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.conect.aplicativoconect.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import java.util.UUID

class AddBookingActivity : AppCompatActivity() {

    private lateinit var editTextName: EditText
    private lateinit var spinnerService: Spinner
    private lateinit var textViewSelectedDate: TextView
    private lateinit var textViewSelectedTime: TextView
    private lateinit var buttonSaveBooking: Button
    private lateinit var companyId: String // Adiciona a variável para armazenar o companyId

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_add_booking)

        editTextName = findViewById(R.id.editTextName)
        spinnerService = findViewById(R.id.spinnerService)
        textViewSelectedDate = findViewById(R.id.textViewSelectedDate)
        textViewSelectedTime = findViewById(R.id.textViewSelectedTime)
        buttonSaveBooking = findViewById(R.id.buttonSaveBooking)

        // Buscar o companyId do usuário logado
        fetchCompanyId()

        buttonSaveBooking.setOnClickListener {
            saveBooking()
        }
    }

    private fun fetchCompanyId() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val userId = user.uid
            FirebaseFirestore.getInstance().collection("business").document(userId)
                .get()
                .addOnSuccessListener { document ->
                    if (document != null && document.exists()) {
                        companyId = document.getString("ownerId") ?: ""
                    } else {
                        Toast.makeText(this, "Usuário não encontrado", Toast.LENGTH_SHORT).show()
                    }
                }
                .addOnFailureListener { exception ->
                    Toast.makeText(
                        this,
                        "Erro ao buscar companyId: ${exception.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        } else {
            Toast.makeText(this, "Usuário não está autenticado", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveBooking() {
        val name = editTextName.text.toString()
        val service = spinnerService.selectedItem.toString()
        val selectedDate = textViewSelectedDate.text.toString()
        val selectedHour = textViewSelectedTime.text.toString()

        if (name.isEmpty() || selectedDate == "Data não selecionada" || selectedHour == "Horário não selecionado") {
            Toast.makeText(this, "Preencha todos os campos!", Toast.LENGTH_SHORT).show()
            return
        }

        uploadImageAndSaveBooking(name, service, selectedDate, selectedHour)
    }

    private fun uploadImageAndSaveBooking(
        name: String,
        service: String,
        date: String,
        hour: String
    ) {
        val storageRef = FirebaseStorage.getInstance().reference
        val imageRef = storageRef.child("bookings_images/sem_agendamentos.png")

        val drawableUri = Uri.parse("android.resource://${packageName}/drawable/sem_agendamentos")

        imageRef.putFile(drawableUri)
            .addOnSuccessListener {
                imageRef.downloadUrl.addOnSuccessListener { uri ->
                    val imageUrl = uri.toString()
                    // Salva o agendamento no Firestore com a URL da imagem e o companyId
                    saveBookingToFirestore(name, service, date, hour, imageUrl)
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
        service: String,
        date: String,
        hour: String,
        imageUrl: String
    ) {
        // Assegure-se de que companyId é do tipo String
        val companyIdValue = if (companyId.isNotBlank()) companyId else ""

        // Crie um HashMap garantindo que os tipos estão corretos
        val bookingData: HashMap<String, Any> = hashMapOf(
            "name" to name,
            "serviceName" to service,
            "date" to date,
            "hour" to hour,
            "userId" to UUID.randomUUID().toString(),
            "companyId" to companyIdValue, // Aqui usamos a variável já verificada
            "status_cliente" to "pending",
            "status_adm" to "pending",
            "imageUrl" to imageUrl
        )

        // Salvar o agendamento no Firestore
        FirebaseFirestore.getInstance().collection("bookings")
            .add(bookingData)
            .addOnSuccessListener {
                Toast.makeText(this, "Agendamento salvo com sucesso!", Toast.LENGTH_SHORT).show()
                finish() // Fecha a Activity após salvar
            }
            .addOnFailureListener { exception ->
                Toast.makeText(this, "Erro ao salvar agendamento: ${exception.message}", Toast.LENGTH_SHORT).show()
            }
    }


}