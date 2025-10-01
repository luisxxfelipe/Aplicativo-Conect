package com.conect.aplicativoconect.ui.fragments

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.ui.activities.PublishPhotoActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class AddServiceDialogFragment : DialogFragment() {

    private lateinit var firestore: FirebaseFirestore
    private var companyId: String? = null  // Variável para armazenar o ID da empresa

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_add_service, container, false)

        firestore = FirebaseFirestore.getInstance()

        // Recupera o UID do usuário logado como companyId
        companyId = FirebaseAuth.getInstance().currentUser?.uid
        if (companyId.isNullOrEmpty()) {
            Toast.makeText(requireContext(), "Erro: Usuário não autenticado.", Toast.LENGTH_SHORT)
                .show()
            dismiss()
            return null
        }

        // Buscar serviços da empresa
        loadCompanyServices(companyId!!)

        // Configurar botões
        val addServiceButton: Button = view.findViewById(R.id.btn_adicionar_servicos)
        val adjustHoursButton: Button = view.findViewById(R.id.btn_ajustar_horario)
        val btnPublicarFotos: Button = view.findViewById(R.id.btn_publicar_fotos)

        addServiceButton.setOnClickListener {
            val fragment = AddServiceFragment().apply {
                arguments = Bundle().apply {
                    putString(
                        "companyId",
                        companyId
                    ) // Passa o companyId diretamente para o fragmento
                }
            }
            val transaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, fragment)
            transaction.addToBackStack(null)
            transaction.commit()
            dismiss()
        }

        adjustHoursButton.setOnClickListener {
            val fragment = OperatingHoursFragment().apply {
                arguments = Bundle().apply {
                    putString(
                        "companyId",
                        companyId
                    ) // Passa o companyId diretamente para o fragmento
                }
            }
            val transaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, fragment)
            transaction.addToBackStack(null)
            transaction.commit()
            dismiss()
        }

        btnPublicarFotos.setOnClickListener {
            val intent = Intent(requireContext(), PublishPhotoActivity::class.java).apply {
                putExtra("companyId", companyId) // Passa o companyId diretamente para a atividade
            }
            startActivity(intent)
            dismiss()
        }

        return view
    }

    private fun loadCompanyServices(companyId: String) {
        firestore.collection("business").document(companyId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    // Recupera os serviços como uma lista de mapas
                    val services = document.get("services") as? List<Map<String, Any>>
                    if (!services.isNullOrEmpty()) {
                        // Processa e exibe os serviços encontrados
                        val serviceList = services.map { service ->
                            val serviceName =
                                service["serviceName"] as? String ?: "Serviço sem nome"
                            val price = (service["price"] as? Number)?.toDouble() ?: 0.0
                            "Serviço: $serviceName, Preço: R$ $price"
                        }
                    }
                } else {
                    Toast.makeText(requireContext(), "Empresa não encontrada.", Toast.LENGTH_SHORT)
                        .show()
                }
            }
            .addOnFailureListener { exception ->
                // Trata erro ao carregar os dados
                Toast.makeText(
                    requireContext(),
                    "Erro ao carregar serviços: ${exception.message}",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(dpToPx(330), dpToPx(320))
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
}
