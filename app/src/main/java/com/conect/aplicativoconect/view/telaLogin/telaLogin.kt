package com.conect.aplicativoconect.view.telaLogin

import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.conect.aplicativoconect.databinding.ActivityTelaLoginBinding
import com.conect.aplicativoconect.view.formcadastro.tela_cadastro
import com.conect.aplicativoconect.view.telaprincipal.telaPrincipal
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException
import com.google.firebase.ktx.Firebase

class telaLogin : AppCompatActivity() {

    private  lateinit var binding: ActivityTelaLoginBinding
    private var auth = FirebaseAuth.getInstance() // recupera a instancia do servidor para autenticar o usuario
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTelaLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.botaoEntrar.setOnClickListener{ view ->
            val email = binding.editEmail.text.toString()
            val senha = binding.editSenha.text.toString()

            if(email.isEmpty() || senha.isEmpty()){
                val snackbar = Snackbar.make(view, "Preencha todos os campos corretamente!", Snackbar.LENGTH_SHORT)
                snackbar.setBackgroundTint(Color. RED)
                snackbar.show()
            }

            else{
                auth.signInWithEmailAndPassword(email,senha).addOnCompleteListener{ autenticacao ->
                    if(autenticacao.isSuccessful){
                        navegarTelaPrincipal()
                    }
                }.addOnFailureListener{exception ->
                    val mensagem_error = when(exception){
                        is FirebaseAuthWeakPasswordException -> "Digite uma senha com pelo menos 6 caracteres!"
                        is FirebaseAuthInvalidCredentialsException -> "Digite um email válido!"
                        is FirebaseAuthUserCollisionException -> "Essa conta já foi cadastrada!"
                        is FirebaseNetworkException -> "Sem conexão com a internet!"
                        else -> "Erro ao fazer o login do usuário"
                    }

                    val snackbar = Snackbar.make(view, mensagem_error, Snackbar.LENGTH_SHORT)
                    snackbar.setBackgroundTint(Color. RED)
                    snackbar.show()
                }
            }
        }

        binding.textTelaCadastro.setOnClickListener{
            val intent = Intent(this, tela_cadastro::class.java)
            startActivity(intent)
        }

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN
        supportActionBar?.hide()
    }

    private fun navegarTelaPrincipal() {
        val intent = Intent(this, telaPrincipal::class.java)
        startActivity(intent)
        finish()
    }

    override fun onStart() {
        super.onStart()
        val usuario_total = FirebaseAuth.getInstance().currentUser // usuario atual que esta logado no sistema

        if(usuario_total != null){
            navegarTelaPrincipal()
        }
    }
}