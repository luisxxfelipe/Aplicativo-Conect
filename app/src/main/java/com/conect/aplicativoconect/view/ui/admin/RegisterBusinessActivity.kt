package com.conect.aplicativoconect.view.ui.admin

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
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
import com.conect.aplicativoconect.view.data.model.Business
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class RegisterBusinessActivity : AppCompatActivity(), OperatingHoursDialogFragment.OnHoursSelectedListener {

    private lateinit var auth: FirebaseAuth
    private lateinit var firestore: FirebaseFirestore

    private lateinit var selectedServiceType: String
    private lateinit var operatingHoursInput: TextInputEditText
    private lateinit var businessImageView: ImageView
    private lateinit var uploadIcon: ImageView
    private var imageUri: Uri? = null // Para armazenar a URI da imagem
    private val storagePermissionCode = 101

    // ActivityResultLauncher para o resultado da galeria
    private lateinit var getContent: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register_business)

        // Inicializar Firebase Auth e Firestore
        auth = FirebaseAuth.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // Referências aos componentes
        val businessNameInput = findViewById<TextInputEditText>(R.id.businessNameInput)
        val serviceTypeSpinner = findViewById<Spinner>(R.id.serviceTypeSpinner)
        val addressInput = findViewById<TextInputEditText>(R.id.addressInput)
        operatingHoursInput = findViewById<TextInputEditText>(R.id.operatingHoursInput)
        val phoneInput = findViewById<TextInputEditText>(R.id.phoneInput)
        val registerBusinessButton = findViewById<MaterialButton>(R.id.registerBusinessButton)

        // Referenciar o ImageView
        businessImageView = findViewById(R.id.businessImageView)
        uploadIcon = findViewById(R.id.uploadButton)

        // Configurar o Spinner com opções de serviços
        val serviceTypes = listOf("Cabeleireiro", "Manicure", "Barbeiro", "Estética", "Massagem")
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

// Adicionar a lógica para abrir a galeria também no clique do botão de upload
        registerBusinessButton.setOnClickListener {
            val businessName = businessNameInput.text.toString().trim()
            val address = addressInput.text.toString().trim()
            val phone = phoneInput.text.toString().trim()

            if (validateInputs(businessName, address, phone)) {
                // Use a imagem padrão se imageUri for nulo
                val finalImageUri = imageUri ?: Uri.parse("android.resource://${packageName}/drawable/default_img") // Substitua pelo ID da imagem padrão
                registerBusiness(businessName, selectedServiceType, address, operatingHoursInput.text.toString(), phone, finalImageUri)
            }
        }
    }

    private fun validateInputs(businessName: String, address: String, phone: String): Boolean {
        return when {
            businessName.isEmpty() -> {
                Toast.makeText(this, "Por favor, insira o nome da empresa.", Toast.LENGTH_SHORT).show()
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
            else -> true
        }
    }

    private fun registerBusiness(businessName: String, serviceType: String, address: String, operatingHours: String, phone: String, imageUri: Uri) {
        // Adicionar lógica para salvar no Firestore, incluindo a imagem se necessário
        val business = Business(businessName, serviceType, address, operatingHours, phone, imageUri.toString())

        firestore.collection("business")
            .add(business)
            .addOnSuccessListener {
                Toast.makeText(this, "Empresa cadastrada com sucesso!", Toast.LENGTH_SHORT).show()
                // Redirecionar ou limpar campos após cadastro
                clearFields()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao cadastrar: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun clearFields() {
        findViewById<TextInputEditText>(R.id.businessNameInput).text?.clear()
        findViewById<TextInputEditText>(R.id.addressInput).text?.clear()
        findViewById<TextInputEditText>(R.id.phoneInput).text?.clear()
        operatingHoursInput.text?.clear()
        imageUri = null
        businessImageView.setImageResource(R.drawable.default_img) // Substitua por um recurso padrão
        businessImageView.visibility = View.GONE
        uploadIcon.visibility = View.VISIBLE
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == storagePermissionCode) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                Toast.makeText(this, "Permissão de armazenamento concedida", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permissão de armazenamento negada", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onHoursSelected(opening: String, closing: String) {
        operatingHoursInput.setText("Abertura: $opening, Fechamento: $closing")
    }
}
