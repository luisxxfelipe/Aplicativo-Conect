package com.conect.aplicativoconect.view.ui.auth

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings.Secure
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
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
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.ui.client.ClienteHomeActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.storage.FirebaseStorage

class SignupClientActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var db: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var phoneEditText: EditText
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
        phoneEditText = findViewById(R.id.phoneInput)
        applyPhoneMask(phoneEditText)
        signUpButton = findViewById(R.id.signupButton)
        loginTextView = findViewById(R.id.loginTextView)
        profileImageView = findViewById(R.id.profileImageView) // Adicione o ImageView no seu layout
        uploadProfileButton = findViewById(R.id.uploadButton) // Adicione o botão de upload

        // Inicializando o ActivityResultLauncher
        getContent =
            registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
                if (result.resultCode == Activity.RESULT_OK) {
                    val data = result.data
                    imageUri = data?.data
                    profileImageView.setImageURI(imageUri) // Exibir a imagem selecionada
                    profileImageView.visibility = View.VISIBLE // Tornar visível a imagem de perfil
                    findViewById<ImageView>(R.id.uploadIcon).visibility =
                        View.GONE // Ocultar o ícone de upload
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
            val phone = phoneEditText.text.toString().trim()
            val confirmPassword = confirmPasswordEditText.text.toString().trim()
            val name = nameEditText.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty() && confirmPassword.isNotEmpty() && name.isNotEmpty() && phone.isNotEmpty()) {

                if (password == confirmPassword) {
                    progressDialog.show() // Mostrar progresso
                    createUser(email, password, name, phone)
                } else {
                    Toast.makeText(this, "As senhas não coincidem", Toast.LENGTH_SHORT).show()
                }
            } else {
                Toast.makeText(this, "Por favor, preencha todos os campos", Toast.LENGTH_SHORT)
                    .show()
            }
        }

        // Navegar para a tela de login ao clicar no TextView
        loginTextView.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
        }
    }

    private fun applyPhoneMask(editText: EditText) {
        editText.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false
            private val mask = "(##) #####-####"

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isUpdating) return
                isUpdating = true

                val unmasked = s.toString().replace("[^\\d]".toRegex(), "")
                val masked = StringBuilder()
                var i = 0

                for (char in mask) {
                    if (char != '#' && unmasked.length > i) {
                        masked.append(char)
                    } else if (i < unmasked.length) {
                        masked.append(unmasked[i])
                        i++
                    }
                }

                editText.setText(masked.toString())
                editText.setSelection(masked.length)
                isUpdating = false
            }

            override fun afterTextChanged(s: Editable?) {}
        })
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

    private fun createUser(email: String, password: String, name: String, phone: String) {
        progressDialog.show() // Mostrar progresso ao iniciar a criação do usuário

        auth.createUserWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: return@addOnCompleteListener

                    // Criação do HashMap com dados do usuário
                    val userData: HashMap<String, Any> = hashMapOf(
                        "userId" to userId,
                        "email" to email,
                        "name" to name,
                        "phoneCliente" to phone,
                        "isActive" to true,
                        "type" to "client"
                    )

                    // Fazer upload da imagem de perfil se houver
                    imageUri?.let {
                        uploadProfileImage(it, userId, userData)
                    } ?: run {
                        saveUserToFirestore(userId, userData) {
                            saveFCMToken(userId) // Salva o token após salvar o usuário
                        }
                    }
                } else {
                    progressDialog.dismiss() // Fechar progresso em caso de erro
                    val exception = task.exception
                    if (exception is FirebaseAuthWeakPasswordException) {
                        Toast.makeText(
                            this,
                            "A senha é muito fraca. Por favor, escolha uma senha mais forte.",
                            Toast.LENGTH_SHORT
                        ).show()
                    } else {
                        Toast.makeText(
                            this,
                            "Falha ao cadastrar. Tente novamente.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
    }

    // Função para obter e salvar o token FCM
    private fun saveFCMToken(userId: String) {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val token = task.result

                // Tenta buscar o documento do usuário
                db.collection("users").document(userId).get()
                    .addOnSuccessListener { document ->
                        if (document.exists()) {
                            // Se o documento existir, apenas atualiza o token
                            db.collection("users").document(userId)
                                .update("fcmToken", token)
                                .addOnSuccessListener {
                                    Log.d("FCM", "Token atualizado com sucesso para $userId.")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("FCM", "Erro ao atualizar token: ${e.message}")
                                }
                        } else {
                            // Se o documento não existir, cria um novo com o token
                            val userData = hashMapOf("fcmToken" to token)
                            db.collection("users").document(userId)
                                .set(userData)
                                .addOnSuccessListener {
                                    Log.d("FCM", "Token criado com sucesso para $userId.")
                                }
                                .addOnFailureListener { e ->
                                    Log.e("FCM", "Erro ao criar token: ${e.message}")
                                }
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.e("FCM", "Erro ao buscar documento: ${e.message}")
                    }
            } else {
                Log.e("FCM", "Erro ao obter token", task.exception)
            }
        }
    }


    private fun uploadProfileImage(imageUri: Uri, userId: String?, userData: HashMap<String, Any>) {
        val storageRef =
            storage.reference.child("profile_images/$userId/${imageUri.lastPathSegment}")
        val uploadTask = storageRef.putFile(imageUri)

        uploadTask.continueWithTask { task ->
            if (!task.isSuccessful) {
                task.exception?.let { throw it }
            }
            storageRef.downloadUrl
        }.addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val downloadUri = task.result
                userData["imageUrl"] =
                    downloadUri.toString() // Adiciona a URL da imagem aos dados do usuário

                // Passando o callback corretamente
                saveUserToFirestore(userId, userData) {
                    saveFCMToken(userId!!) // Após salvar o usuário, salva o token FCM
                }
            } else {
                Toast.makeText(this, "Falha ao obter URL da imagem.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun saveUserToFirestore(
        userId: String?,
        userData: HashMap<String, Any>,
        onSuccess: () -> Unit
    ) {
        userId?.let {
            db.collection("users").document(it).set(userData)
                .addOnSuccessListener {
                    Toast.makeText(this, "Cadastro bem-sucedido!", Toast.LENGTH_SHORT).show()
                    onSuccess() // Chama o callback após o sucesso
                    val intent = Intent(this, ClienteHomeActivity::class.java)
                    intent.putExtra("FROM_SIGNUP", true) // Adicione o extra
                    startActivity(intent)
                    finish()
                }
                .addOnFailureListener { e ->
                    Log.e("Firestore", "Erro ao salvar o usuário: ${e.message}")
                    Toast.makeText(
                        this,
                        "Falha ao salvar usuário. Tente novamente.",
                        Toast.LENGTH_SHORT
                    ).show()
                }
        }
    }
}