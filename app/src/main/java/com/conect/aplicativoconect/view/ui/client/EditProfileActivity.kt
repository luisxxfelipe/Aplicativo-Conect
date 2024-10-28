package com.conect.aplicativoconect.view.ui.client

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        // Inicializa as views
        editProfileImage = findViewById(R.id.editProfileImage)
        editUserName = findViewById(R.id.editUserName)
        editUserEmail = findViewById(R.id.editUserEmail)
        saveProfileButton = findViewById(R.id.saveProfileButton)

        loadUserProfile()

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

        val user = firebaseAuth.currentUser ?: return
        val profileUpdates = userProfileChangeRequest {
            displayName = name
            photoUri = selectedPhotoUri ?: user.photoUrl
        }

        user.updateProfile(profileUpdates).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this, "Perfil atualizado", Toast.LENGTH_SHORT).show()
                finish() // Voltar para a tela anterior
            } else {
                Toast.makeText(this, "Erro ao atualizar perfil", Toast.LENGTH_SHORT).show()
            }
        }

        user.updateEmail(email).addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Toast.makeText(this, "Erro ao atualizar e-mail", Toast.LENGTH_SHORT).show()
            }
        }

        selectedPhotoUri?.let { uri ->
            val ref = storage.reference.child("profileImages/${user.uid}")
            ref.putFile(uri).addOnSuccessListener {
                ref.downloadUrl.addOnSuccessListener { downloadUri ->
                    user.updateProfile(userProfileChangeRequest { photoUri = downloadUri })
                }
            }
        }
    }
}
