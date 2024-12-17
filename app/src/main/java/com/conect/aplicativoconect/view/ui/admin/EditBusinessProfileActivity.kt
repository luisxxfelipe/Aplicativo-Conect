package com.conect.aplicativoconect.view.ui.admin

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
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class EditBusinessProfileActivity : AppCompatActivity() {

    private val firebaseAuth = FirebaseAuth.getInstance()
    private val firestore = FirebaseFirestore.getInstance()
    private val storage = FirebaseStorage.getInstance()
    private var selectedPhotoUri: Uri? = null

    private lateinit var editBusinessImage: de.hdodenhof.circleimageview.CircleImageView
    private lateinit var editOwnerName: com.google.android.material.textfield.TextInputEditText
    private lateinit var editBusinessName: com.google.android.material.textfield.TextInputEditText
    private lateinit var editBusinessEmail: com.google.android.material.textfield.TextInputEditText
    private lateinit var editBusinessAddress: com.google.android.material.textfield.TextInputEditText
    private lateinit var editBusinessDescription: com.google.android.material.textfield.TextInputEditText
    private lateinit var editBusinessCpf: com.google.android.material.textfield.TextInputEditText
    private lateinit var editBusinessPhone: com.google.android.material.textfield.TextInputEditText
    private lateinit var saveBusinessProfileButton: com.google.android.material.button.MaterialButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_business_profile)

        // Inicializa as views
        editBusinessImage = findViewById(R.id.editBusinessImage)
        editOwnerName = findViewById(R.id.editOwnerName)
        editBusinessName = findViewById(R.id.editBusinessName)
        editBusinessEmail = findViewById(R.id.editBusinessEmail)
        editBusinessAddress = findViewById(R.id.editBusinessAddress)
        editBusinessDescription = findViewById(R.id.editBusinessDescription)
        editBusinessCpf = findViewById(R.id.editBusinessCpf)
        editBusinessPhone = findViewById(R.id.editBusinessPhone)
        saveBusinessProfileButton = findViewById(R.id.saveBusinessProfileButton)

        // Bloquear o campo de e-mail
        editBusinessEmail.isFocusable = false
        editBusinessEmail.isCursorVisible = false
        editBusinessEmail.setOnTouchListener { _, _ ->
            Toast.makeText(
                this,
                "O e-mail não pode ser modificado.",
                Toast.LENGTH_SHORT
            ).show()
            true // Consome o evento e impede qualquer interação
        }

        loadBusinessProfile()
        applyCpfMask()
        applyPhoneMask()

        editBusinessImage.setOnClickListener {
            selectPhotoFromGallery()
        }

        saveBusinessProfileButton.setOnClickListener {
            saveBusinessProfile()
        }
    }


    private fun applyCpfMask() {
        editBusinessCpf.addTextChangedListener(object : TextWatcher {
            private var isUpdating = false
            private val mask = "###.###.###-##"
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
                editBusinessCpf.setText(maskedStr.toString())
                editBusinessCpf.setSelection(maskedStr.length)
            }
        })
    }

    private fun applyPhoneMask() {
        editBusinessPhone.addTextChangedListener(object : TextWatcher {
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
                editBusinessPhone.setText(maskedStr.toString())
                editBusinessPhone.setSelection(maskedStr.length)
            }
        })
    }

    private fun loadBusinessProfile() {
        val userId = firebaseAuth.currentUser?.uid ?: return

        firestore.collection("business").document(userId).get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    editOwnerName.setText(document.getString("ownerName"))
                    editBusinessName.setText(document.getString("name"))
                    editBusinessEmail.setText(document.getString("email"))
                    editBusinessAddress.setText(document.getString("address"))
                    editBusinessDescription.setText(document.getString("description"))
                    editBusinessCpf.setText(document.getString("cpf"))
                    editBusinessPhone.setText(document.getString("phone"))

                    Glide.with(this)
                        .load(document.getString("imageUrl"))
                        .placeholder(R.drawable.foto_perfil_generica)
                        .into(editBusinessImage)
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

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 0 && resultCode == Activity.RESULT_OK && data != null) {
            selectedPhotoUri = data.data
            Glide.with(this).load(selectedPhotoUri).into(editBusinessImage)
        }
    }

    private fun saveBusinessProfile() {
        val userId = firebaseAuth.currentUser?.uid ?: return

        val updatedData = mapOf(
            "ownerName" to editOwnerName.text.toString(),
            "name" to editBusinessName.text.toString(),
            "email" to editBusinessEmail.text.toString(),
            "address" to editBusinessAddress.text.toString(),
            "description" to editBusinessDescription.text.toString(),
            "cpf" to editBusinessCpf.text.toString(),
            "phone" to editBusinessPhone.text.toString()
        )

        firestore.collection("business").document(userId).update(updatedData)
            .addOnSuccessListener {
                selectedPhotoUri?.let { uri ->
                    val ref = storage.reference.child("business_images/$userId")
                    ref.putFile(uri).addOnSuccessListener {
                        ref.downloadUrl.addOnSuccessListener { downloadUri ->
                            firestore.collection("business").document(userId)
                                .update("imageUrl", downloadUri.toString())
                        }
                    }
                }
                Toast.makeText(this, "Perfil atualizado com sucesso", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Erro ao atualizar perfil", Toast.LENGTH_SHORT).show()
            }
    }
}