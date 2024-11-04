package com.conect.aplicativoconect.view.ui.admin

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

class AddServiceDialogFragment : DialogFragment() {

    private var companyId: String? = null  // Adiciona variável para armazenar o ID da empresa

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

        // Obtém o companyId do argumento se estiver presente
        companyId = arguments?.getString("companyId")

        // Configurar botões
        val addServiceButton: Button = view.findViewById(R.id.btn_adicionar_servicos)
        val adjustHoursButton: Button = view.findViewById(R.id.btn_ajustar_horario)
        val btnPublicarFotos: Button = view.findViewById(R.id.btn_publicar_fotos)

        addServiceButton.setOnClickListener {
            val fragment = AddServiceFragment()
            val transaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, fragment)
            transaction.addToBackStack(null)
            transaction.commit()
            dismiss()
        }

        adjustHoursButton.setOnClickListener {
            val fragment = OperatingHoursFragment()  // Use o nome correto da classe
            val transaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, fragment)
            transaction.addToBackStack(null)
            transaction.commit()
            dismiss()
        }

        btnPublicarFotos.setOnClickListener {
            if (companyId != null) {
                val intent = Intent(requireContext(), PublishPhotoActivity::class.java).apply {
                    putExtra("companyId", companyId)  // Passa o companyId para PublishPhotoActivity
                }
                startActivity(intent)
            } else {
                Toast.makeText(
                    requireContext(),
                    "ID da empresa não encontrado.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            dismiss()
        }

        return view
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(dpToPx(330), dpToPx(250))
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP,
            dp.toFloat(),
            resources.displayMetrics
        ).toInt()
    }
}
