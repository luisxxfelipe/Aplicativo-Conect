package com.conect.aplicativoconect.view.ui.client

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.conect.aplicativoconect.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.userProfileChangeRequest
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class EditProfileActivity : AppCompatActivity() {

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private var selectedPhotoUri: Uri? = null

    private lateinit var editProfileImage: de.hdodenhof.circleimageview.CircleImageView
    private lateinit var editUserName: com.google.android.material.textfield.TextInputEditText
    private lateinit var editUserEmail: com.google.android.material.textfield.TextInputEditText
    private lateinit var saveProfileButton: com.google.android.material.button.MaterialButton
    private lateinit var editUserPhone: com.google.android.material.textfield.TextInputEditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        // Inicializa as views
        editProfileImage = findViewById(R.id.editProfileImage)
        editUserName = findViewById(R.id.editUserName)
        editUserEmail = findViewById(R.id.editUserEmail)
        saveProfileButton = findViewById(R.id.saveProfileButton)
        editUserPhone = findViewById(R.id.editUserPhone)

        loadUserProfile()
        // Máscara para Telefone
        editUserPhone.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false
            private val mask = "(##) #####-####"
            override fun afterTextChanged(s: Editable?) {}
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (isUpdating) {
                    isUpdating = false
                    return
                }

                var str = s.toString().replace(Regex("[^\\d]"), "")
                val maskedStr = StringBuilder()
                var index = 0
                for (m in mask.toCharArray()) {
                    if (m != '#' && index < str.length) {
                        maskedStr.append(m)
                        continue
                    }
                    if (index >= str.length) break
                    maskedStr.append(str[index])
                    index++
                }

                isUpdating = true
                editUserPhone.setText(maskedStr.toString())
                editUserPhone.setSelection(maskedStr.length)
            }
        })

        editProfileImage.setOnClickListener {
            selectPhotoFromGallery()
        }

        saveProfileButton.setOnClickListener {
            saveUserProfile()
        }
    }

    private fun loadUserProfile() {
        val userId = firebaseAuth.currentUser?.uid ?: return

        firestore.collection("users").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    editUserName.setText(document.getString("name"))
                    editUserEmail.setText(document.getString("email"))
                    editUserPhone.setText(document.getString("phoneCliente"))

                    val imageUrl = document.getString("imageUrl")
                    Glide.with(this)
                        .load(imageUrl)
                        .placeholder(R.drawable.foto_perfil_generica)
                        .error(R.drawable.foto_perfil_generica)
                        .into(editProfileImage)
                }
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao carregar perfil", Toast.LENGTH_SHORT).show()
            }
    }

    private fun selectPhotoFromGallery() {
        val intent = Intent(Intent.ACTION_PICK).apply { type = "image/*" }
        startActivityForResult(intent, 0)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0 && resultCode == Activity.RESULT_OK && data != null) {
            selectedPhotoUri = data.data
            Glide.with(this).load(selectedPhotoUri).into(editProfileImage)
        }
    }

    private fun saveUserProfile() {
        val name = editUserName.text.toString()
        val email = editUserEmail.text.toString()
        val phone = editUserPhone.text.toString()
        val user = firebaseAuth.currentUser ?: return

        // Atualizar as informações no Firebase Authentication
        val profileUpdates = userProfileChangeRequest {
            displayName = name
            photoUri = selectedPhotoUri ?: user.photoUrl
        }

        user.updateProfile(profileUpdates).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                updateFirestoreData(user.uid, name, email, phone)
            } else {
                Toast.makeText(this, "Erro ao atualizar perfil", Toast.LENGTH_SHORT).show()
            }
        }

        // Atualizar o e-mail do usuário
        user.updateEmail(email).addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Toast.makeText(this, "Erro ao atualizar e-mail", Toast.LENGTH_SHORT).show()
            }
        }

        // Upload da foto de perfil para o Firebase Storage
        selectedPhotoUri?.let { uri ->
            val ref = storage.reference.child("profileImages/${user.uid}")
            ref.putFile(uri).addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { downloadUri ->
                    user.updateProfile(userProfileChangeRequest { photoUri = downloadUri })
                    updateFirestoreData(user.uid, name, email, phone, downloadUri.toString())
                }
            }
        }
    }

    private fun updateFirestoreData(
        userId: String,
        name: String,
        email: String,
        phone: String,
        imageUrl: String? = null
    ) {
        val userData = mutableMapOf<String, Any>(
            "name" to name,
            "email" to email,
            "phoneCliente" to phone
        )
        imageUrl?.let { userData["imageUrl"] = it }

        firestore.collection("users").document(userId).update(userData)
            .addOnSuccessListener {
                Toast.makeText(this, "Perfil atualizado com sucesso", Toast.LENGTH_SHORT).show()
                finish() // Voltar para a tela de perfil
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao atualizar no Firestore", Toast.LENGTH_SHORT).show()
            }
    }
}
