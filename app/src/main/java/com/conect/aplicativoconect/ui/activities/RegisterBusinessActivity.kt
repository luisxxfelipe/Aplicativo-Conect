package com.conect.aplicativoconect.ui.activities

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.location.Geocoder
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.data.models.OperatingHours
import com.conect.aplicativoconect.ui.fragments.OperatingHoursDialogFragment
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

class RegisterBusinessActivity : AppCompatActivity(),
    OperatingHoursDialogFragment.OnHoursSelectedListener {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var selectedServiceType: String
    private lateinit var operatingHoursInput: TextInputEditText
    private lateinit var businessImageView: ImageView
    private lateinit var uploadIcon: ImageView
    private var imageUri: Uri? = null
    private lateinit var email: String
    private lateinit var storage: FirebaseStorage
    private lateinit var selectedOperatingHours: OperatingHours
    private lateinit var progressDialog: AlertDialog

    // ActivityResultLauncher para o resultado da galeria
    private lateinit var getContent: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_business)

        // Inicializar Firebase Auth e Firestore
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        // Inicializar o AlertDialog personalizado
        setupProgressDialog()

        // Recuperar o email passado pela SignupBusinessActivity
        email = intent.getStringExtra("EMAIL_KEY") ?: ""

        // Referências aos componentes
        val businessNameInput = findViewById<TextInputEditText>(R.id.nameUser)
        val businessDescriptionInput =
            findViewById<TextInputEditText>(R.id.businessDescriptionInput)
        val serviceTypeSpinner = findViewById<Spinner>(R.id.serviceTypeSpinner)
        val dropdownIcon = findViewById<ImageView>(R.id.dropdownIcon)
        val addressInput = findViewById<TextInputEditText>(R.id.addressInput)
        operatingHoursInput = findViewById(R.id.operatingHoursInput)
        val phoneInput = findViewById<TextInputEditText>(R.id.phoneInput)
        val registerBusinessButton = findViewById<MaterialButton>(R.id.registerBusinessButton)

        // Preencher nome automaticamente se vier do Google
        val googleName = intent.getStringExtra("GOOGLE_NAME") ?: ""
        if (googleName.isNotEmpty()) {
            businessNameInput.setText(googleName)
        }

        // Adicionando a máscara de telefone
        phoneInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {}

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s != null) {
                    // Verifica o comprimento da string e aplica a máscara
                    val formatted = formatPhoneNumber(s.toString())
                    if (s.toString() != formatted) {
                        phoneInput.setText(formatted)
                        phoneInput.setSelection(formatted.length)
                    }
                }
            }
        })

        // Referenciar o ImageView
        businessImageView = findViewById(R.id.businessImageView)
        uploadIcon = findViewById(R.id.uploadIcon)

        // Configurar o Spinner com opções de serviços
        val serviceTypes =
            listOf("Cabeleireiro", "Manicure", "Estética", "Barbeiro", "Massagem", "Técnico de Informática", "Fotógrafo", "Depilação", "Desenvolvedor de sites")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, serviceTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        serviceTypeSpinner.adapter = adapter

        serviceTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                selectedServiceType = serviceTypes[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedServiceType = "Cabeleireiro"
            }
        }

        // Fazer o ícone de dropdown também abrir o spinner
        dropdownIcon.setOnClickListener {
            serviceTypeSpinner.performClick()
        }

        // Configurar o clique para abrir o diálogo de horários de funcionamento
        operatingHoursInput.setOnClickListener {
            val dialog = OperatingHoursDialogFragment()
            dialog.setOnHoursSelectedListener(this)
            dialog.show(supportFragmentManager, "OperatingHoursDialog")
        }

        // Inicializando o ActivityResultLauncher
        getContent =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val data = result.data
                    imageUri = data?.data
                    if (imageUri != null) {
                        businessImageView.setImageURI(imageUri)
                        businessImageView.visibility = View.VISIBLE
                        uploadIcon.visibility = View.GONE
                    } else {
                        Toast.makeText(this, "Erro ao obter a imagem.", Toast.LENGTH_SHORT).show()
                    }
                }
            }

        // Configurar o clique para selecionar imagem no ícone de upload
        uploadIcon.setOnClickListener {
            openGallery()
        }

        // Configurar o clique para selecionar imagem no ImageView (opcional)
        businessImageView.setOnClickListener {
            openGallery()
        }

        registerBusinessButton.setOnClickListener {
            val businessName = businessNameInput.text.toString().trim()
            val businessDescription = businessDescriptionInput.text.toString().trim()
            val address = addressInput.text.toString().trim()
            val phone = phoneInput.text.toString().trim()

            // Validação dos inputs
            if (!validateInputs(
                    businessName,
                    businessDescription,
                    address,
                    phone,
                    email
                )
            ) return@setOnClickListener

            // Verifica se há uma imagem selecionada, caso contrário exibe mensagem
            if (imageUri == null) {
                Toast.makeText(
                    this,
                    "Por favor, selecione uma imagem para a empresa.",
                    Toast.LENGTH_SHORT
                ).show()
                return@setOnClickListener
            }

            // Exibe o progresso
            progressDialog.show()

            // Obtem coordenadas e registra a empresa
            fetchCoordinatesAsync(address) { lat, lng, city ->
                if (lat != null && lng != null && city != null) {
                    registerBusiness(
                        businessName = businessName,
                        businessDescription = businessDescription,
                        serviceType = selectedServiceType,
                        address = address,
                        latitude = lat,
                        longitude = lng,
                        city = city,
                        operatingHours = selectedOperatingHours,
                        phone = phone,
                        imageUri = imageUri!!, // Certificamos que não será nulo
                        email = email
                    )
                } else {
                    progressDialog.dismiss()
                    Toast.makeText(
                        this,
                        "Endereço inválido ou cidade não encontrada. Verifique o endereço e tente novamente.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }
    }

    private fun formatPhoneNumber(phone: String): String {
        var cleanPhone = phone.replace("[^\\d]".toRegex(), "") // Remove tudo que não for número

        // Aplica a máscara à medida que o número vai sendo digitado
        return when {
            cleanPhone.length <= 2 -> {
                "($cleanPhone"
            }

            cleanPhone.length in 3..6 -> {
                "(${cleanPhone.substring(0, 2)}) ${cleanPhone.substring(2)}"
            }

            cleanPhone.length in 7..10 -> {
                "(${cleanPhone.substring(0, 2)}) ${
                    cleanPhone.substring(
                        2,
                        7
                    )
                }-${cleanPhone.substring(7)}"
            }

            else -> {
                "(${cleanPhone.substring(0, 2)}) ${
                    cleanPhone.substring(
                        2,
                        7
                    )
                }-${cleanPhone.substring(7, 11)}"
            }
        }
    }


    private fun setupProgressDialog() {
        val builder = AlertDialog.Builder(this)
        val inflater = LayoutInflater.from(this)
        val view =
            inflater.inflate(R.layout.dialog_progress, null) // Layout personalizado do diálogo

        builder.setView(view)
        builder.setCancelable(false) // Impede que o usuário feche o diálogo

        progressDialog = builder.create()
    }

    private fun validateInputs(
        businessName: String,
        businessDescription: String,
        address: String,
        phone: String,
        email: String
    ): Boolean {
        var isValid = true

        if (businessName.isEmpty()) {
            findViewById<TextInputEditText>(R.id.nameUser).error = "Campo obrigatório"
            isValid = false
        } else {
            findViewById<TextInputEditText>(R.id.nameUser).error = null
        }

        if (businessDescription.isEmpty()) {
            findViewById<TextInputEditText>(R.id.businessDescriptionInput).error = "Campo obrigatório"
            isValid = false
        } else {
            findViewById<TextInputEditText>(R.id.businessDescriptionInput).error = null
        }

        if (address.isEmpty()) {
            findViewById<TextInputEditText>(R.id.addressInput).error = "Campo obrigatório"
            isValid = false
        } else {
            findViewById<TextInputEditText>(R.id.addressInput).error = null
        }

        if (phone.isEmpty()) {
            findViewById<TextInputEditText>(R.id.phoneInput).error = "Campo obrigatório"
            isValid = false
        } else {
            findViewById<TextInputEditText>(R.id.phoneInput).error = null
        }

        if (email.isEmpty()) {
            Toast.makeText(this, "E-mail é obrigatório!", Toast.LENGTH_SHORT).show()
            isValid = false
        }

        return isValid
    }

    private fun registerBusiness(
        businessName: String,
        businessDescription: String,
        serviceType: String,
        address: String,
        latitude: Double,
        longitude: Double,
        city: String, // Adicione o parâmetro de cidade
        operatingHours: OperatingHours,
        phone: String,
        imageUri: Uri,
        email: String
    ) {
        val userId = auth.currentUser?.uid ?: return // UID do usuário autenticado

        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val fcmToken = task.result

                val storageRef =
                    storage.reference.child("business_images/$userId/${imageUri.lastPathSegment}")

                val uploadTask = storageRef.putFile(imageUri)
                uploadTask.continueWithTask { task ->
                    if (!task.isSuccessful) {
                        task.exception?.let { throw it }
                    }
                    storageRef.downloadUrl // Obtém a URL do download
                }.addOnCompleteListener { task ->
                    progressDialog.dismiss()
                    if (task.isSuccessful) {
                        val downloadUri = task.result // A URL correta está aqui

                        val businessData = mapOf(
                            "name" to businessName,
                            "description" to businessDescription,
                            "serviceType" to serviceType,
                            "address" to address,
                            "latitude" to latitude,
                            "longitude" to longitude,
                            "city" to city,
                            "phone" to phone,
                            "operatingHours" to operatingHours,
                            "imageUrl" to downloadUri.toString(), // URL corrigida
                            "email" to email,
                            "fcmToken" to fcmToken
                        )

                        val businessRef = firestore.collection("business").document(userId)

                        // Verifica se o documento de negócios já existe
                        businessRef.get().addOnSuccessListener { documentSnapshot ->
                            if (documentSnapshot.exists()) {
                                // Documento existe, então atualiza os dados
                                businessRef.update(businessData)
                                    .addOnSuccessListener {
                                        Toast.makeText(
                                            this,
                                            "Empresa atualizada com sucesso!",
                                            Toast.LENGTH_SHORT
                                        ).show()

                                        // Exibe o popup antes de redirecionar
                                        showWelcomeDialog {
                                            val intent = Intent(this, AdminHomeActivity::class.java)
                                            intent.putExtra("FROM_REGISTER", true)
                                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                            startActivity(intent)
                                            finish()
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(
                                            this,
                                            "Erro ao atualizar: ${e.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            }
                            else {
                                // Documento não existe, cria o novo
                                businessRef.set(businessData)
                                    .addOnSuccessListener {
                                        Toast.makeText(
                                            this,
                                            "Empresa cadastrada com sucesso!",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        val intent = Intent(this, AdminHomeActivity::class.java)
                                        intent.putExtra("FROM_REGISTER", true)
                                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                        startActivity(intent)
                                        finish()
                                    }
                                    .addOnFailureListener { e ->
                                        Toast.makeText(
                                            this,
                                            "Erro ao cadastrar: ${e.message}",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                    }
                            }
                        }.addOnFailureListener { e ->
                            Toast.makeText(
                                this,
                                "Erro ao verificar dados da empresa: ${e.message}",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        Toast.makeText(this, "Falha ao obter URL da imagem.", Toast.LENGTH_SHORT)
                            .show()
                    }
                }
            }
        }
    }


    private fun fetchCoordinatesAsync(
        address: String,
        callback: (Double?, Double?, String?) -> Unit
    ) {
        val geocoder = Geocoder(this, Locale.getDefault())
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val addresses = geocoder.getFromLocationName(address, 1)
                withContext(Dispatchers.Main) {
                    if (!addresses.isNullOrEmpty()) {
                        val location = addresses[0]
                        val cityName =
                            location.locality ?: location.subAdminArea // Obtém o nome da cidade
                        callback(location.latitude, location.longitude, cityName)
                    } else {
                        callback(null, null, null)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback(null, null, null)
                }
            }
        }
    }

    private fun showWelcomeDialog(onDismiss: () -> Unit) {
        val builder = AlertDialog.Builder(this)
        val dialog = builder.setTitle("Parabéns por começar a usar o Conectex")
            .setMessage(
                "Como responsável pelo estabelecimento, você terá 1 mês gratuito para explorar todas as funcionalidades. Após esse período, será cobrado R$ 27,00 por mês para continuar o uso. Lembre-se: para os clientes, " +
                        "o uso do app é totalmente gratuito! Aproveite e conte com a gente para ajudar seu negócio a crescer!"
            )
            .setPositiveButton("Continuar") { dialog, _ ->
                dialog.dismiss()
                onDismiss() // Chama a função passada após o fechamento
            }
            .create()

        dialog.setOnShowListener {
            val positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE)
            positiveButton.setTextColor(ContextCompat.getColor(this, R.color.roxo))
        }

        dialog.setCancelable(false) // Evita que o usuário feche o popup clicando fora
        dialog.show()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT)
        intent.type = "image/*"
        getContent.launch(intent)
    }

    override fun onHoursSelected(opening: String, closing: String) {
        selectedOperatingHours = OperatingHours(
            opening = opening,
            closing = closing,
            days = listOf() // Atualize esta lista com os dias de funcionamento selecionados, se houver
        )
        val operatingHoursText = "$opening - $closing"
        operatingHoursInput.setText(operatingHoursText)
    }

}
