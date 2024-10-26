package com.conect.aplicativoconect.view.ui.auth

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.ui.client.ClienteHomeActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class SignupClientActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var nameEditText: EditText
    private lateinit var signUpButton: Button
    private lateinit var loginTextView: TextView
    private lateinit var profileImageView: ImageView
    private lateinit var uploadProfileButton: Button
    private lateinit var progressDialog: AlertDialog
    private var imageUri: Uri? = null
    private lateinit var getContent: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup_client)

        auth = FirebaseAuth.getInstance()
        db = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()

        setupProgressDialog() // Inicializa o progresso

        emailEditText = findViewById(R.id.emailInput)
        passwordEditText = findViewById(R.id.passwordInput)
        confirmPasswordEditText = findViewById(R.id.confirmPasswordInput)
        nameEditText = findViewById(R.id.nameInput)
        signUpButton = findViewById(R.id.signupButton)
        loginTextView = findViewById(R.id.loginTextView)
        profileImageView = findViewById(R.id.profileImageView) // Adicione o ImageView no seu layout
        uploadProfileButton = findViewById(R.id.uploadButton) // Adicione o botão de upload

        // Inicializando o ActivityResultLauncher
        getContent = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                imageUri = data?.data
                profileImageView.setImageURI(imageUri) // Exibir a imagem selecionada
                profileImageView.visibility = View.VISIBLE // Tornar visível a imagem de perfil
                findViewById<ImageView>(R.id.uploadIcon).visibility = View.GONE // Ocultar o ícone de upload
            } else {
                Toast.makeText(this, "Erro ao obter a imagem.", Toast.LENGTH_SHORT).show()
            }
        }

        // Abrir galeria para selecionar imagem ao clicar no ImageView
        profileImageView.setOnClickListener {
            openGallery()
        }

        // Abrir galeria para selecionar imagem ao clicar no botão de upload
        uploadProfileButton.setOnClickListener {
            openGallery()
        }

        signUpButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()
            val name = nameEditText.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty() && name.isNotEmpty()) {
                if (password == confirmPassword) {
                    progressDialog.show() // Mostrar progresso
                    createUser(email, password, name)
                } else {
                    Toast.makeText(this, "As senhas não coincidem", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Por favor, preencha todos os campos", Toast.LENGTH_SHORT).show()
            }
        }

        // Navegar para a tela de login ao clicar no TextView
        loginTextView.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun setupProgressDialog() {
        val builder = AlertDialog.Builder(this)
        val inflater = LayoutInflater.from(this)
        val view = inflater.inflate(R.layout.dialog_progress_cliente, null)
        builder.setView(view)
        builder.setCancelable(false)
        progressDialog = builder.create()
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = "image/*"
        getContent.launch(intent)
    }

    private fun createUser(email: String, password: String, name: String) {
        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Cadastro bem-sucedido, salve os dados do usuário no Firestore
                    val userId = auth.currentUser?.uid
                    // Certifique-se de que os valores são do tipo Any
                    val userData = hashMapOf<String, Any>(
                        "email" to email,
                        "name" to name,
                        "isActive" to true,
                        "type" to "client"
                    )

                    // Fazer o upload da imagem de perfil, se houver
                    imageUri?.let {
                        uploadProfileImage(it, userId, userData)
                    } ?: run {
                        // Se não houver imagem, apenas salvar os dados do usuário
                        saveUserToFirestore(userId, userData)
                    }
                } else {
                    progressDialog.dismiss() // Esconder progresso em caso de erro
                    Toast.makeText(baseContext, "Falha ao cadastrar. Tente novamente.", Toast.LENGTH_SHORT).show()
                }
            }
    }


    private fun uploadProfileImage(imageUri: Uri, userId: String?, userData: HashMap<String, Any>) {
        val storageRef = storage.reference.child("profile_images/$userId/${imageUri.lastPathSegment}")
        val uploadTask = storageRef.putFile(imageUri)
        uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let { throw it }
            }
            // Após o upload, obter a URL de download
            storageRef.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result
                userData["imageUrl"] = downloadUri.toString() // Adiciona a URL da imagem aos dados do usuário
                saveUserToFirestore(userId, userData)
            } else {
                Toast.makeText(this, "Falha ao obter URL da imagem.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveUserToFirestore(userId: String?, userData: HashMap<String, Any>) {
        userId?.let {
            db.collection("users").document(it).set(userData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Cadastro bem-sucedido!", Toast.LENGTH_SHORT).show()
                    startActivity(Intent(this, ClienteHomeActivity::class.java))
                    finish()
                }
                .addOnFailureListener {
                    progressDialog.dismiss() // Esconder progresso em caso de erro
                    Toast.makeText(this, "Falha ao salvar dados do usuário.", Toast.LENGTH_SHORT).show()
                }
        }
    }
}
