package com.conect.aplicativoconect.ui.activities

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.ui.adapters.PhotoAdapter
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage

class PublishPhotoActivity : AppCompatActivity() {

    private lateinit var companyId: String
    private val selectedPhotoUris = mutableListOf<Uri>()
    private lateinit var firestore: FirebaseFirestore
    private lateinit var storage: FirebaseStorage
    private lateinit var progressBar: ProgressBar
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PhotoAdapter
    private var photosUploaded = 0 // Contador de fotos enviadas

    companion object {
        private const val PHOTO_PICKER_REQUEST_CODE = 1001
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_publish_photo)

        companyId = intent.getStringExtra("companyId") ?: ""
        if (companyId.isBlank()) {
            Toast.makeText(this, "ID da empresa não encontrado", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        firestore = FirebaseFirestore.getInstance()
        storage = FirebaseStorage.getInstance()
        progressBar = findViewById(R.id.progressBar)
        recyclerView = findViewById(R.id.recyclerViewSelectedPhotos)

        // Configuração do RecyclerView para mostrar fotos em três colunas
        recyclerView.layoutManager = GridLayoutManager(this, 3)
        adapter = PhotoAdapter(selectedPhotoUris)
        recyclerView.adapter = adapter

        findViewById<FloatingActionButton>(R.id.buttonAddPhoto).setOnClickListener { openGallery() }
        findViewById<Button>(R.id.buttonSavePhoto).setOnClickListener { uploadPhotosWithCaption() }
    }

    private fun openGallery() {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = "image/*"
        }
        startActivityForResult(intent, PHOTO_PICKER_REQUEST_CODE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == PHOTO_PICKER_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val photoUri = data?.data
            if (photoUri != null) {
                selectedPhotoUris.add(photoUri)
                adapter.notifyDataSetChanged()  // Atualiza o RecyclerView
                findViewById<Button>(R.id.buttonSavePhoto).isEnabled = true
            }
        }
    }

    private fun uploadPhotosWithCaption() {
        val caption = findViewById<EditText>(R.id.editTextPhotoTitle).text.toString().trim()

        if (selectedPhotoUris.isEmpty()) {
            Toast.makeText(this, "Selecione pelo menos uma foto!", Toast.LENGTH_SHORT).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        photosUploaded = 0 // Reinicia o contador

        selectedPhotoUris.forEach { photoUri ->
            val ref =
                storage.reference.child("service_photos/$companyId/${photoUri.lastPathSegment}")
            ref.putFile(photoUri)
                .addOnSuccessListener {
                    ref.downloadUrl.addOnSuccessListener { downloadUrl ->
                        savePhotoInfoToFirestore(downloadUrl.toString(), caption)
                    }
                }
                .addOnFailureListener {
                    progressBar.visibility = View.GONE
                    Toast.makeText(this, "Erro ao fazer upload da foto.", Toast.LENGTH_SHORT).show()
                }
        }
    }

    private fun savePhotoInfoToFirestore(photoUrl: String, caption: String) {
        val photoData = hashMapOf("url" to photoUrl, "caption" to caption)

        firestore.collection("business").document(companyId)
            .collection("servicePhotos")
            .add(photoData)
            .addOnSuccessListener {
                photosUploaded++ // Incrementa o contador de fotos enviadas
                if (photosUploaded == selectedPhotoUris.size) { // Checa se todas as fotos foram enviadas
                    progressBar.visibility = View.GONE
                    Toast.makeText(
                        this,
                        "Todas as fotos foram salvas com sucesso!",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            }
            .addOnFailureListener {
                progressBar.visibility = View.GONE
                Toast.makeText(this, "Erro ao salvar a foto no Firestore.", Toast.LENGTH_SHORT)
                    .show()
            }
    }
}
