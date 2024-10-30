package com.conect.aplicativoconect.view.ui.admin

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.data.model.OperatingHours
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage

class RegisterBusinessActivity : AppCompatActivity(), OperatingHoursDialogFragment.OnHoursSelectedListener {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore
    private lateinit var selectedServiceType: String
    private lateinit var operatingHoursInput: TextInputEditText
    private lateinit var businessImageView: ImageView
    private lateinit var uploadIcon: MaterialButton
    private var imageUri: Uri? = null
    private val storagePermissionCode = 101
    private lateinit var email: String
    private lateinit var storage: FirebaseStorage
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
        val businessDescriptionInput = findViewById<TextInputEditText>(R.id.businessDescriptionInput)
        val serviceTypeSpinner = findViewById<Spinner>(R.id.serviceTypeSpinner)
        val addressInput = findViewById<TextInputEditText>(R.id.addressInput)
        operatingHoursInput = findViewById(R.id.operatingHoursInput)
        val phoneInput = findViewById<TextInputEditText>(R.id.phoneInput)
        val registerBusinessButton = findViewById<MaterialButton>(R.id.registerBusinessButton)

        // Referenciar o ImageView
        businessImageView = findViewById(R.id.businessImageView)
        uploadIcon = findViewById(R.id.uploadButton)

        // Configurar o Spinner com opções de serviços
        val serviceTypes = listOf("Cabeleireiro", "Manicure", "Estética", "Cabeleireiro", "Massagem")
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, serviceTypes)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        serviceTypeSpinner.adapter = adapter

        serviceTypeSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
                selectedServiceType = serviceTypes[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
                selectedServiceType = ""
            }
        }

        // Configurar o clique para abrir o diálogo de horários de funcionamento
        operatingHoursInput.setOnClickListener {
            val dialog = OperatingHoursDialogFragment()
            dialog.setOnHoursSelectedListener(this)
            dialog.show(supportFragmentManager, "OperatingHoursDialog")
        }

        // Verifica a permissão ao iniciar a Activity
        if (!checkStoragePermission()) {
            requestStoragePermission()
        }

        // Inicializando o ActivityResultLauncher
        getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
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

        // Configurar o clique para selecionar imagem no ImageView
        businessImageView.setOnClickListener {
            openGallery()
        }

        // Configurar o clique para o botão de upload
        uploadIcon.setOnClickListener {
            openGallery() // Abre a galeria ao clicar no ícone de upload
        }

        registerBusinessButton.setOnClickListener {
            val businessName = businessNameInput.text.toString().trim()
            val businessDescription = businessDescriptionInput.text.toString().trim()
            val address = addressInput.text.toString().trim()
            val phone = phoneInput.text.toString().trim()

            if (validateInputs(businessName, businessDescription, address, phone, email)) {
                // Use a imagem padrão se imageUri for nulo
                val finalImageUri = imageUri ?: Uri.parse("android.resource://${packageName}/drawable/default_img")

                // Criar uma instância de OperatingHours
                val operatingHours = OperatingHours(
                    opening = "08:00",  // Horário de abertura padrão
                    closing = "18:00",  // Horário de fechamento padrão
                    days = listOf()      // Aqui você pode passar a lista de dias selecionados
                )

                progressDialog.show() // Mostrar o diálogo de progresso

                // Registrar o negócio
                registerBusiness(
                    businessName,
                    businessDescription,
                    selectedServiceType,
                    address,
                    operatingHours,
                    phone,
                    finalImageUri,
                    email // Passando o email aqui
                )
            }
        }
    }

    private fun setupProgressDialog() {
        val builder = AlertDialog.Builder(this)
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.dialog_progress, null) // Layout personalizado do diálogo

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
        return when {
            businessName.isEmpty() -> {
                Toast.makeText(this, "Por favor, insira o nome da empresa.", Toast.LENGTH_SHORT).show()
                false
            }
            businessDescription.isEmpty() -> {
                Toast.makeText(this, "Por favor, insira a descrição da empresa.", Toast.LENGTH_SHORT).show()
                false
            }
            address.isEmpty() -> {
                Toast.makeText(this, "Por favor, insira o endereço.", Toast.LENGTH_SHORT).show()
                false
            }
            phone.isEmpty() -> {
                Toast.makeText(this, "Por favor, insira o telefone.", Toast.LENGTH_SHORT).show()
                false
            }
            email.isEmpty() -> {
                Toast.makeText(this, "Por favor, insira o email.", Toast.LENGTH_SHORT).show()
                false
            }
            else -> true
        }
    }

    private fun registerBusiness(
        businessName: String,
        businessDescription: String,
        serviceType: String,
        address: String,
        operatingHours: OperatingHours,
        phone: String,
        imageUri: Uri,
        email: String
    ) {
        val userId = auth.currentUser?.uid ?: return // UID do usuário autenticado

        // Obter o token FCM
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val fcmToken = task.result

                // Definir o caminho para salvar a imagem no Firebase Storage
                val storageRef = storage.reference.child("business_images/$userId/${imageUri.lastPathSegment}")

                // Fazer o upload da imagem para o Firebase Storage
                val uploadTask = storageRef.putFile(imageUri)
                uploadTask.continueWithTask { uploadTask ->
                    if (!uploadTask.isSuccessful) {
                        uploadTask.exception?.let { throw it }
                    }
                    storageRef.downloadUrl
                }.addOnCompleteListener { uploadTask ->
                    progressDialog.dismiss()
                    if (uploadTask.isSuccessful) {
                        val downloadUri = uploadTask.result

                        // Cria ou atualiza o documento da empresa com o token FCM
                        val business = mapOf(
                            "name" to businessName,
                            "description" to businessDescription,
                            "serviceType" to serviceType,
                            "address" to address,
                            "phone" to phone,
                            "operatingHours" to operatingHours,
                            "imageUrl" to downloadUri.toString(),
                            "email" to email,
                            "isActive" to true,
                            "ownerId" to userId, // UID do usuário autenticado
                            "fcmToken" to fcmToken // Token FCM do proprietário
                        )

                        // Usa `set` para garantir que o documento seja atualizado ou criado
                        firestore.collection("business").document(userId)
                            .set(business)
                            .addOnSuccessListener {
                                Toast.makeText(this, "Empresa cadastrada com sucesso!", Toast.LENGTH_SHORT).show()
                                val intent = Intent(this, AdminHomeActivity::class.java)
                                startActivity(intent)
                                finish()
                            }
                            .addOnFailureListener { e ->
                                Toast.makeText(this, "Erro ao cadastrar: ${e.message}", Toast.LENGTH_SHORT).show()
                            }
                    } else {
                        Toast.makeText(this, "Falha ao obter URL da imagem.", Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                Toast.makeText(this, "Erro ao obter token FCM.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        getContent.launch(intent) // Usando o ActivityResultLauncher
    }

    private fun checkStoragePermission(): Boolean {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestStoragePermission() {
        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), storagePermissionCode)
    }

    // Callback para selecionar horários
    override fun onHoursSelected(openingTime: String, closingTime: String) {
        val operatingHoursText = "$openingTime - $closingTime"
        operatingHoursInput.setText(operatingHoursText)
    }
}
