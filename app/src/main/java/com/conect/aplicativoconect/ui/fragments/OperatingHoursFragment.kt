package com.conect.aplicativoconect.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TimePicker
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Observer
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.ui.activities.AdminHomeActivity
import com.google.firebase.auth.FirebaseAuth
import com.conect.aplicativoconect.data.repositories.CompanyRepository
import com.conect.aplicativoconect.ui.viewmodels.CompanyViewModel

class OperatingHoursFragment : Fragment() {

    private lateinit var timePickerOpen: TimePicker
    private lateinit var timePickerClose: TimePicker
    private lateinit var saveHoursButton: Button
    private lateinit var auth: FirebaseAuth
    private val companyViewModel: CompanyViewModel by activityViewModels()

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

        // Inicializar FirebaseAuth
        auth = FirebaseAuth.getInstance()

        // Configuração dos botões de dias da semana
        setupDayButtons(view)

        // Setup observers
        setupObservers()
        
        // Carregar horários registrados
        val userId = auth.currentUser?.uid
        if (userId != null) {
            companyViewModel.loadOperatingHours(userId)
        }

        saveHoursButton.setOnClickListener {
            saveOperatingHours()
        }
    }

    private fun setupObservers() {
        companyViewModel.operatingHours.observe(viewLifecycleOwner) { operatingHours ->
            operatingHours?.let {
                // Configurar horários de abertura e fechamento
                val opening = it.opening.split(":").let { parts ->
                    parts[0].toIntOrNull() to parts.getOrNull(1)?.toIntOrNull()
                }
                val closing = it.closing.split(":").let { parts ->
                    parts[0].toIntOrNull() to parts.getOrNull(1)?.toIntOrNull()
                }

                timePickerOpen.hour = opening.first ?: 7
                timePickerOpen.minute = opening.second ?: 0
                timePickerClose.hour = closing.first ?: 19
                timePickerClose.minute = closing.second ?: 0

                // Configurar dias da semana
                selectedDays.clear()
                selectedDays.addAll(it.days)
                updateDayButtons()
            }
        }
        
        companyViewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(context, it, Toast.LENGTH_SHORT).show()
            }
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
                    button.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.orange))
                    button.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                } else {
                    // Quando não selecionado: fundo cinza e texto preto
                    button.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.cinza_claro))
                    button.setTextColor(ContextCompat.getColor(requireContext(), R.color.cinza_escuro))
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
                toggleDaySelection(day) // ✅ OTIMIZADO: Removido parâmetro desnecessário
            }
        }
    }

    private fun toggleDaySelection(day: String) { // ✅ OTIMIZADO: Removido parâmetro não utilizado
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

        // Criar objeto OperatingHours
        val operatingHours = CompanyRepository.OperatingHours(
            opening = openingHour,
            closing = closingHour,
            days = selectedDays.toList()
        )

        // Salvar usando o ViewModel
        companyViewModel.saveOperatingHours(userId, operatingHours)
        
        // Observer para success será chamado automaticamente
        companyViewModel.operatingHours.observe(viewLifecycleOwner) { savedHours ->
            if (savedHours != null && savedHours == operatingHours) {
                Toast.makeText(context, "Horários salvos com sucesso!", Toast.LENGTH_SHORT).show()
                redirectToHome()
            }
        }
    }

    private fun redirectToHome() {
        val intent = Intent(activity, AdminHomeActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        activity?.finish()
    }
}
