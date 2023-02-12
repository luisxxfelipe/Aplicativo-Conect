package com.conect.aplicativoconect.view.formcadastro

import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import com.conect.aplicativoconect.databinding.ActivityTelaCadastroBinding
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.FirebaseNetworkException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.auth.FirebaseAuthWeakPasswordException

class tela_cadastro : AppCompatActivity() {

    private lateinit var binding: ActivityTelaCadastroBinding
    private val auth = FirebaseAuth.getInstance()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityTelaCadastroBinding.inflate(layoutInflater)
        setContentView(binding.root)

        binding.botaoCadastrar.setOnClickListener { view ->
            val email = binding.cadastroEmail.text.toString() // salvando na variavel email e senha o que o usuario insere
            val senha = binding.cadastroSenha.text.toString()

            if (email.isEmpty() || senha.isEmpty()) { // caso senha e email seja diferente de vazio

                val snackbar = Snackbar.make( view, "Preencha todos os campos corretamente!", Snackbar.LENGTH_SHORT)
                snackbar.setBackgroundTint(Color.RED)
                snackbar.show()

            } else {
                auth.createUserWithEmailAndPassword(email,senha).addOnCompleteListener{ cadastro->
                    if(cadastro.isSuccessful){ // caso o cadastro seja feito com sucesso
                        val snackbar = Snackbar.make(view, "Usuário criado com sucesso!", Snackbar.LENGTH_SHORT)
                        snackbar.setBackgroundTint(Color.BLUE)
                        snackbar.show()

                        binding.cadastroEmail.setText("")
                        binding.cadastroSenha.setText("")
                    }

                }.addOnFailureListener { exception->
                    val mensagemErro = when(exception){
                        is FirebaseAuthWeakPasswordException -> "Digite uma senha com pelo menos 6 caracteres!"
                        is FirebaseAuthInvalidCredentialsException -> "Digite um email válido!"
                        is FirebaseAuthUserCollisionException -> "Essa conta já foi cadastrada!"
                        is FirebaseNetworkException -> "Sem conexão com a internet!"
                        else -> "Erro ao cadastrar usuário!"
                    }

                    val snackbar = Snackbar.make( view, mensagemErro, Snackbar.LENGTH_SHORT) // comando para monstrar mensagem de ao usuario
                    snackbar.setBackgroundTint(Color.RED)
                    snackbar.show()
                }
            }
        }

        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_FULLSCREEN // comando para retirar o statusBar
        supportActionBar?.hide()
    }
}