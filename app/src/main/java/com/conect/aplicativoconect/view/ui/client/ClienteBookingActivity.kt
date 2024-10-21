package com.conect.aplicativoconect.view.ui.client

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.conect.aplicativoconect.R
import com.google.android.material.button.MaterialButton

class ClienteBookingActivity : AppCompatActivity() {

    private lateinit var addBookingButton: MaterialButton
    private lateinit var bookingsRecyclerView: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_client_booking)

        addBookingButton = findViewById(R.id.addBookingButton)
        bookingsRecyclerView = findViewById(R.id.bookingsRecyclerView)

        // Lógica para adicionar agendamentos e configurar RecyclerView
        addBookingButton.setOnClickListener {
            // Ação para adicionar agendamentos
        }

        // Configuração do RecyclerView
        setupRecyclerView()
    }

    private fun setupRecyclerView() {
        // Configuração do RecyclerView
    }
}
