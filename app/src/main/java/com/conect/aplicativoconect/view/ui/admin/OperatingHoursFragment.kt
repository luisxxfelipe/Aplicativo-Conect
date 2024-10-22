package com.conect.aplicativoconect.view.ui.admin

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TimePicker
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.ui.admin.AdminHomeActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions

class OperatingHoursFragment : Fragment() {

    private lateinit var timePickerOpen: TimePicker
    private lateinit var timePickerClose: TimePicker
    private lateinit var saveHoursButton: Button
    private lateinit var firestore: FirebaseFirestore
    private lateinit var auth: FirebaseAuth

    private val selectedDays = mutableSetOf<String>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_operating_hours, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        timePickerOpen = view.findViewById(R.id.timePickerOpen)
        timePickerClose = view.findViewById(R.id.timePickerClose)
        saveHoursButton = view.findViewById(R.id.saveHoursButton)

        // Inicializar Firestore e FirebaseAuth
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        saveHoursButton.setOnClickListener {
            saveOperatingHours()
        }

        // Configuração dos botões de dias da semana
        setupDayButtons(view)
    }

    private fun setupDayButtons(view: View) {
        val buttons = mapOf(
            "Monday" to view.findViewById<Button>(R.id.buttonMonday),
            "Tuesday" to view.findViewById<Button>(R.id.buttonTuesday),
            "Wednesday" to view.findViewById<Button>(R.id.buttonWednesday),
            "Thursday" to view.findViewById<Button>(R.id.buttonThursday),
            "Friday" to view.findViewById<Button>(R.id.buttonFriday),
            "Saturday" to view.findViewById<Button>(R.id.buttonSaturday),
            "Sunday" to view.findViewById<Button>(R.id.buttonSunday)
        )

        buttons.forEach { (day, button) ->
            button.setOnClickListener {
                toggleDaySelection(day, button)
            }
        }
    }

    private fun toggleDaySelection(day: String, button: Button) {
        if (selectedDays.contains(day)) {
            // Desmarcar o dia
            selectedDays.remove(day)
            button.setBackgroundResource(R.drawable.button_default)
            button.setTextColor(resources.getColor(R.color.black))
        } else {
            // Marcar o dia
            selectedDays.add(day)
            button.setBackgroundResource(R.drawable.button_selected)
            button.setTextColor(resources.getColor(R.color.white))
        }
    }

    private fun saveOperatingHours() {
        val openingHour = "${timePickerOpen.hour}:00" // Formato "HH:mm"
        val closingHour = "${timePickerClose.hour}:00" // Formato "HH:mm"

        // Capturando o ID do usuário
        val userId = auth.currentUser?.uid ?: return Toast.makeText(context, "Usuário não autenticado", Toast.LENGTH_SHORT).show()

        // Lógica para salvar os horários no Firestore
        val hours = hashMapOf(
            "opening" to openingHour,
            "closing" to closingHour,
            "days" to selectedDays.toList() // Adicionando os dias selecionados
        )

        // Aqui usamos o mesmo userId para atualizar o documento correspondente
        firestore.collection("business")
            .document(userId) // Use o ID do usuário como identificador
            .set(mapOf("operatingHours" to hours), SetOptions.merge())
            .addOnSuccessListener {
                Toast.makeText(context, "Horários salvos com sucesso!", Toast.LENGTH_SHORT).show()
                redirectToHome()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Erro ao salvar horários: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }


    private fun redirectToHome() {
        val intent = Intent(activity, AdminHomeActivity::class.java)
        startActivity(intent)
        activity?.finish()
    }
}
