package com.conect.aplicativoconect.view.ui.admin

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
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        return dialog
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_operating_hours, container, false)

        openingHoursInput = view.findViewById(R.id.openingHoursInput)
        closingHoursInput = view.findViewById(R.id.closingHoursInput)
        val saveButton = view.findViewById<Button>(R.id.saveButton)

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

            // Verificação se os horários estão preenchidos
            if (opening.isEmpty() || closing.isEmpty()) {
                Toast.makeText(context, "Por favor, insira os horários", Toast.LENGTH_SHORT).show()
            } else {
                listener?.onHoursSelected(opening, closing)
                dismiss()
            }
        }

        return view
    }

    private fun showTimePicker(onTimeSelected: (String) -> Unit) {
        val calendar = Calendar.getInstance()
        val hour = calendar.get(Calendar.HOUR_OF_DAY)
        val minute = calendar.get(Calendar.MINUTE)

        TimePickerDialog(
            requireContext(),
            { _: TimePicker, selectedHour: Int, selectedMinute: Int ->
                val formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute)
                onTimeSelected(formattedTime)
            },
            hour,
            minute,
            true
        ).show()
    }
}
