package com.conect.aplicativoconect.ui.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.utils.TokenManager

import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider

class LoginActivity : AppCompatActivity() {
    private var userType: String = "client"

    private lateinit var auth: com.google.firebase.auth.FirebaseAuth
    private lateinit var emailEditText: EditText
    private lateinit var passwordEditText: EditText
    private lateinit var loginButton: Button
    private lateinit var signUpButton: android.widget.TextView
    private lateinit var rememberMeCheckBox: android.widget.CheckBox
    private val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()

    private lateinit var googleSignInClient: GoogleSignInClient
    private val RC_SIGN_IN = 9001

    override fun onCreate(savedInstanceState: Bundle?) {
        // Recupera tipo do usuário (Intent ou SharedPreferences)
        userType = intent.getStringExtra("USER_TYPE") ?: run {
            val sharedPref = getSharedPreferences("userTypePrefs", MODE_PRIVATE)
            sharedPref.getString("USER_TYPE", "client") ?: "client"
        }
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        // Configurar textos de boas-vindas baseado no userType
        val welcomeTitle = findViewById<android.widget.TextView>(R.id.welcomeTitle)
        val welcomeSubtitle = findViewById<android.widget.TextView>(R.id.welcomeSubtitle)
        when (userType) {
            "client" -> {
                welcomeSubtitle.text = "Encontre os melhores serviços próximos a você"
            }
            "business" -> {
                welcomeSubtitle.text = "Gerencie seu negócio com facilidade"
            }
            else -> {
                welcomeSubtitle.text = "Bem-vindo ao Conectex"
            }
        }

        auth = com.google.firebase.auth.FirebaseAuth.getInstance()

        emailEditText = findViewById(R.id.emailInput)
        passwordEditText = findViewById(R.id.passwordInput)
        loginButton = findViewById(R.id.loginButton)
        signUpButton = findViewById(R.id.signupButton)
        rememberMeCheckBox = findViewById(R.id.rememberMe)

        // SharedPreferences para lembrar login
        val sharedPref = getSharedPreferences("loginPrefs", MODE_PRIVATE)
        val isRemembered = sharedPref.getBoolean("rememberMe", false)
        if (isRemembered) {
            emailEditText.setText(sharedPref.getString("email", ""))
            passwordEditText.setText(sharedPref.getString("password", ""))
            rememberMeCheckBox.isChecked = true
        }

        rememberMeCheckBox.setOnCheckedChangeListener { _, isChecked ->
            val editor = sharedPref.edit()
            if (isChecked) {
                editor.putBoolean("rememberMe", true)
                editor.putString("email", emailEditText.text.toString())
                editor.putString("password", passwordEditText.text.toString())
            } else {
                editor.clear()
            }
            editor.apply()
        }

        // Configuração do Google Sign-In
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val googleLoginButton: ImageButton = findViewById(R.id.googleLoginButton)
        googleLoginButton.setOnClickListener {
            val signInIntent = googleSignInClient.signInIntent
            startActivityForResult(signInIntent, RC_SIGN_IN)
        }

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString().trim()
            val password = passwordEditText.text.toString().trim()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                loginUser(email, password)
            } else {
                Toast.makeText(this, "Por favor, insira email e senha", Toast.LENGTH_SHORT).show()
            }
        }

        signUpButton.setOnClickListener {
            val userType = intent.getStringExtra("USER_TYPE") ?: "client" // Padrão para cliente
            val intent = if (userType == "client") {
                Intent(this, SignupClientActivity::class.java)
            } else {
                Intent(this, SignupBusinessActivity::class.java)
            }
            startActivity(intent)
        }

        val forgotPasswordText: android.widget.TextView = findViewById(R.id.forgotPassword)

        forgotPasswordText.setOnClickListener {
            // Ao clicar no texto "Esqueceu a senha?", vamos abrir a tela de recuperação
            val intent = Intent(this, ForgotPasswordActivity::class.java)
            startActivity(intent)
        }

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(account)
            } catch (e: ApiException) {
                Toast.makeText(this, "Falha no login com Google: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount?) {
        if (acct == null) {
            Toast.makeText(this, "Conta Google inválida.", Toast.LENGTH_SHORT).show()
            return
        }
        val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
                    // Verifica se existe na coleção correta
                    val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                    val collection = if (userType == "client") "users" else "business"
                    db.collection(collection).document(userId).get()
                        .addOnSuccessListener { document ->
                            if (document.exists()) {
                                verifyUserType(userId)
                            } else {
                                // Redireciona para cadastro correto, preenchendo dados do Google
                                val intent = if (userType == "client") {
                                    Intent(this, SignupClientActivity::class.java)
                                } else {
                                    Intent(this, SignupBusinessActivity::class.java)
                                }
                                intent.putExtra("GOOGLE_NAME", acct.displayName ?: "")
                                intent.putExtra("GOOGLE_EMAIL", acct.email ?: "")
                                intent.putExtra("GOOGLE_PHOTO", acct.photoUrl?.toString() ?: "")
                                intent.putExtra("USER_TYPE", userType)
                                startActivity(intent)
                                finish()
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Erro ao verificar usuário.", Toast.LENGTH_SHORT).show()
                        }
                } else {
                    Toast.makeText(this, "Falha na autenticação Google.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun loginUser(email: String, password: String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val userId = auth.currentUser?.uid ?: return@addOnCompleteListener
                    verifyUserType(userId)
                    // Salva login se lembrar-me estiver marcado
                    if (rememberMeCheckBox.isChecked) {
                        val sharedPref = getSharedPreferences("loginPrefs", MODE_PRIVATE)
                        sharedPref.edit()
                            .putBoolean("rememberMe", true)
                            .putString("email", email)
                            .putString("password", password)
                            .apply()
                    }
                } else {
                    val exception = task.exception
                    val errorMessage = when {
                        exception?.message?.contains("password is invalid") == true || 
                        exception?.message?.contains("wrong-password") == true -> 
                            "Senha incorreta. Tente novamente ou use 'Esqueci minha senha'."
                        
                        exception?.message?.contains("user not found") == true ||
                        exception?.message?.contains("no user record") == true -> 
                            "Email não encontrado. Verifique o email ou cadastre-se."
                        
                        exception?.message?.contains("invalid email") == true -> 
                            "Email inválido. Verifique o formato do email."
                        
                        exception?.message?.contains("network error") == true -> 
                            "Erro de conexão. Verifique sua internet."
                            
                        else -> "Falha na autenticação: ${exception?.message ?: "Verifique suas credenciais"}"
                    }
                    
                    Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                }
            }
    }

    private fun verifyUserType(userId: String) {
        // Primeiramente, verifica se o usuário está na coleção "users"
        db.collection("users").document(userId).get()
            .addOnSuccessListener { userDocument ->
                if (userDocument.exists()) {
                    val userType = userDocument.getString("type") ?: "client"

                    // Usuário existe na collection "users" - sempre vai para ClienteHome
                    val intent = Intent(this, ClienteHomeActivity::class.java).apply {
                        putExtra("FORCE_UPDATE", true)
                        flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    }
                    startActivity(intent)
                    finish()
                } else {
                    // Caso o usuário não exista em "users", verifica em "business"
                    db.collection("business").document(userId).get()
                        .addOnSuccessListener { businessDocument ->
                            if (businessDocument.exists()) {
                                // Usuário é business - vai para AdminHome
                                startActivity(Intent(this@LoginActivity, AdminHomeActivity::class.java).apply {
                                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                })
                                finish()
                            } else {
                                // Usuário não existe nem em "users" nem em "business"
                                // Redirecionar para tela de boas-vindas para escolher tipo
                                Toast.makeText(this, "Complete seu cadastro para continuar.", Toast.LENGTH_SHORT)
                                    .show()
                                startActivity(Intent(this@LoginActivity, WelcomeActivity::class.java))
                                finish()
                            }
                        }
                        .addOnFailureListener { e ->
                            Toast.makeText(
                                this,
                                "Erro ao recuperar dados do negócio.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "Erro ao recuperar dados do usuário.", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private fun reAuthenticateAndRedirect(userId: String) {
        auth.signInWithEmailAndPassword(
            emailEditText.text.toString().trim(),
            passwordEditText.text.toString().trim()
        ).addOnCompleteListener { signInTask ->
            if (signInTask.isSuccessful) {
                // Após reautenticar, verifica o tipo de usuário e redireciona
                verifyUserType(userId)  // Essa função já faz o redirecionamento para a home correta
            } else {
                Toast.makeText(
                    this@LoginActivity,
                    "Erro ao re-autenticar após pagamento.",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }
    }


}
