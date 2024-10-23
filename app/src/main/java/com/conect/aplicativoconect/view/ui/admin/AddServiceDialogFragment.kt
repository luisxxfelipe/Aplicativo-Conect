package com.conect.aplicativoconect.view.ui.admin

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.DialogFragment
import com.conect.aplicativoconect.R

class AddServiceDialogFragment : DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT)) // Fundo transparente
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_add_service, container, false)

        // Configurar botões
        val addServiceButton: Button = view.findViewById(R.id.btn_adicionar_servicos)
        val adjustHoursButton: Button = view.findViewById(R.id.btn_ajustar_horario)

        addServiceButton.setOnClickListener {
            // Navegar para a tela de adicionar serviços
            val fragment = AddServiceFragment()
            val transaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, fragment) // Substitua R.id.fragment_container pela ID do seu contêiner de fragmentos
            transaction.addToBackStack(null)
            transaction.commit()
            dismiss() // Fechar o popup
        }

        adjustHoursButton.setOnClickListener {
            // Navegar para a tela de ajustar horário de funcionamento
            val fragment = OperatingHoursFragment() // Substitua pelo nome correto da sua classe
            val transaction = requireActivity().supportFragmentManager.beginTransaction()
            transaction.replace(R.id.fragment_container, fragment) // Substitua R.id.fragment_container pela ID do seu contêiner de fragmentos
            transaction.addToBackStack(null)
            transaction.commit()
            dismiss() // Fechar o popup
        }

        return view
    }

    override fun onStart() {
        super.onStart()
        // Ajuste a largura e a altura do dialog
        dialog?.window?.setLayout(dpToPx(330), dpToPx(250))
    }

    private fun dpToPx(dp: Int): Int {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), resources.displayMetrics).toInt()
    }
}
