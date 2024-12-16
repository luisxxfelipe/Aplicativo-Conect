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

        // Configurar o TimePicker no formato 24 horas
        timePickerOpen.setIs24HourView(true)
        timePickerClose.setIs24HourView(true)

        // Inicializar Firestore e FirebaseAuth
        firestore = FirebaseFirestore.getInstance()
        auth = FirebaseAuth.getInstance()

        // Configuração dos botões de dias da semana
        setupDayButtons(view)

        // Carregar horários registrados
        loadOperatingHours()

        saveHoursButton.setOnClickListener {
            saveOperatingHours()
        }
    }

    private fun loadOperatingHours() {
        val userId = auth.currentUser?.uid ?: return

        firestore.collection("business")
            .document(userId)
            .get()
            .addOnSuccessListener { document ->
                if (document.exists()) {
                    val operatingHours = document.get("operatingHours") as? Map<*, *>
                    if (operatingHours != null) {
                        // Configurar horários de abertura e fechamento
                        val opening = (operatingHours["opening"] as? String)?.split(":") ?: listOf("7", "00")
                        val closing = (operatingHours["closing"] as? String)?.split(":") ?: listOf("19", "00")

                        timePickerOpen.hour = opening[0].toInt()
                        timePickerOpen.minute = opening[1].toInt()
                        timePickerClose.hour = closing[0].toInt()
                        timePickerClose.minute = closing[1].toInt()

                        // Configurar dias da semana
                        val days = operatingHours["days"] as? List<*>
                        if (days != null) {
                            selectedDays.clear()
                            selectedDays.addAll(days.filterIsInstance<String>())

                            // Atualizar aparência dos botões de dias
                            updateDayButtons()
                        }
                    }
                }
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Erro ao carregar horários: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun updateDayButtons() {
        val dayButtons = mapOf(
            "Monday" to view?.findViewById<Button>(R.id.buttonMonday),
            "Tuesday" to view?.findViewById<Button>(R.id.buttonTuesday),
            "Wednesday" to view?.findViewById<Button>(R.id.buttonWednesday),
            "Thursday" to view?.findViewById<Button>(R.id.buttonThursday),
            "Friday" to view?.findViewById<Button>(R.id.buttonFriday),
            "Saturday" to view?.findViewById<Button>(R.id.buttonSaturday),
            "Sunday" to view?.findViewById<Button>(R.id.buttonSunday)
        )

        dayButtons.forEach { (day, button) ->
            button?.let {
                if (selectedDays.contains(day)) {
                    // Quando selecionado: fundo azul e texto branco
                    button.setBackgroundColor(resources.getColor(R.color.orange))
                    button.setTextColor(resources.getColor(R.color.white))
                } else {
                    // Quando não selecionado: fundo cinza e texto preto
                    button.setBackgroundColor(resources.getColor(R.color.cinza_claro))
                    button.setTextColor(resources.getColor(R.color.cinza_escuro))
                }
            }
        }
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
        } else {
            // Marcar o dia
            selectedDays.add(day)
        }
        updateDayButtons() // Atualizar aparência de todos os botões
    }

    private fun saveOperatingHours() {
        val openingHour = "${timePickerOpen.hour}:00" // Formato "HH:mm"
        val closingHour = "${timePickerClose.hour}:00" // Formato "HH:mm"

        // Capturando o ID do usuário
        val userId = auth.currentUser?.uid ?: return Toast.makeText(
            context,
            "Usuário não autenticado",
            Toast.LENGTH_SHORT
        ).show()

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
                Toast.makeText(context, "Erro ao salvar horários: ${e.message}", Toast.LENGTH_SHORT)
                    .show()
            }
    }

    private fun redirectToHome() {
        val intent = Intent(activity, AdminHomeActivity::class.java)
        startActivity(intent)
        activity?.finish()
    }
}
