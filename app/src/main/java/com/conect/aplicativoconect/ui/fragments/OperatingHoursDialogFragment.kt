package com.conect.aplicativoconect.ui.fragments

import android.app.AlertDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TimePicker
import android.widget.Toast
import androidx.fragment.app.DialogFragment
import com.conect.aplicativoconect.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import java.util.Calendar

class OperatingHoursDialogFragment : DialogFragment() {

    private var listener: OnHoursSelectedListener? = null

    interface OnHoursSelectedListener {
        fun onHoursSelected(opening: String, closing: String)
    }

    fun setOnHoursSelectedListener(listener: OnHoursSelectedListener) {
        this.listener = listener
    }

    private lateinit var openingHoursInput: TextInputEditText
    private lateinit var closingHoursInput: TextInputEditText

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_operating_hours, null)

        builder.setView(view)

        openingHoursInput = view.findViewById(R.id.openingHoursInput)
        closingHoursInput = view.findViewById(R.id.closingHoursInput)
        val saveButton = view.findViewById<MaterialButton>(R.id.saveButton)

        openingHoursInput.setOnClickListener {
            showTimePicker { time ->
                openingHoursInput.setText(time)
            }
        }

        closingHoursInput.setOnClickListener {
            showTimePicker { time ->
                closingHoursInput.setText(time)
            }
        }

        saveButton.setOnClickListener {
            val opening = openingHoursInput.text.toString()
            val closing = closingHoursInput.text.toString()

            if (opening.isNotEmpty() && closing.isNotEmpty()) {
                listener?.onHoursSelected(opening, closing)
                dismiss()
            } else {
                Toast.makeText(requireContext(), "Por favor, selecione ambos os horários", Toast.LENGTH_SHORT).show()
            }
        }

        val dialog = builder.create()
        dialog.window?.apply {
            setBackgroundDrawableResource(android.R.color.transparent)
            setDimAmount(0.5f) // Fundo semi-transparente
        }
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return null // Não precisamos desta implementação já que estamos usando onCreateDialog
    }

    private fun showTimePicker(onTimeSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        val timePickerDialog = TimePickerDialog(
            requireContext(),
            { _: TimePicker, selectedHour: Int, selectedMinute: Int ->
                val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                onTimeSelected(formattedTime)
            },
            hour,
            minute,
            true
        )

        timePickerDialog.show()
    }
}
