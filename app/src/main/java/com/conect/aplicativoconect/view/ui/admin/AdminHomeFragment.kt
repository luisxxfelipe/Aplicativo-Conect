package com.conect.aplicativoconect.view.ui.admin

import BookingAdapter
import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.data.model.Business
import com.conect.aplicativoconect.view.data.repository.BookingRepository
import com.conect.aplicativoconect.view.viewmodel.AdminViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class AdminHomeFragment : Fragment() {

    private lateinit var todayBookingsRecyclerView: RecyclerView
    private lateinit var todayBookingsCountTextView: TextView
    private lateinit var monthBookingsCountTextView: TextView
    private lateinit var todayProfitTextView: TextView
    private lateinit var monthProfitTextView: TextView
    private lateinit var noBookingsMessage: TextView
    private lateinit var noBookingsImage: ImageView
    private lateinit var userNameTextView: TextView
    private lateinit var greetingTextView: TextView

    private val bookingRepository = BookingRepository()
    private lateinit var firestore: FirebaseFirestore

    private val adminViewModel: AdminViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_admin_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Inicializa todos os componentes da View
        todayBookingsRecyclerView = view.findViewById(R.id.todayBookingsRecyclerView)
        todayBookingsCountTextView = view.findViewById(R.id.todayBookingsCount)
        monthBookingsCountTextView = view.findViewById(R.id.monthBookingsCount)
        todayProfitTextView = view.findViewById(R.id.todayProfitTextView)
        monthProfitTextView = view.findViewById(R.id.monthProfitTextView)
        noBookingsMessage = view.findViewById(R.id.noBookingsMessage)
        noBookingsImage = view.findViewById(R.id.noBookingsImage)
        userNameTextView = view.findViewById(R.id.userName_business)
        greetingTextView = view.findViewById(R.id.greetingTextView)

        setupRecyclerView()
        loadData()
        loadBusinessName()

        // Observa os dados do ViewModel
        adminViewModel.todayBookingsCount.observe(viewLifecycleOwner) { count ->
            todayBookingsCountTextView.text = count.toString()
        }

        adminViewModel.monthBookingsCount.observe(viewLifecycleOwner) { count ->
            monthBookingsCountTextView.text = count.toString()
        }

        adminViewModel.todayProfit.observe(viewLifecycleOwner) { profit ->
            todayProfitTextView.text = String.format("R$ %.2f", profit)
        }

        adminViewModel.monthProfit.observe(viewLifecycleOwner) { profit ->
            monthProfitTextView.text = String.format("R$ %.2f", profit)
        }

        adminViewModel.businessName.observe(viewLifecycleOwner) { name ->
            userNameTextView.text = name ?: "Nome não disponível"
            setGreeting(name ?: "Usuário")
        }
    }

    private fun setGreeting(name: String) {
        val calendar = Calendar.getInstance()
        val hourOfDay = calendar.get(Calendar.HOUR_OF_DAY)

        val greeting = when {
            hourOfDay < 6 -> "Boa madrugada!"
            hourOfDay < 12 -> "Bom dia!"
            hourOfDay < 18 -> "Boa tarde!"
            else -> "Boa noite!"
        }

        greetingTextView.text = greeting // Atualiza o TextView de saudação
    }

    private fun setupRecyclerView() {
        todayBookingsRecyclerView.layoutManager = LinearLayoutManager(context)
    }

    private fun loadData() {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val weeklyBookings = bookingRepository.getWeeklyBookings()
                Log.d(
                    "AdminHomeFragment",
                    "Agendamentos da semana carregados: ${weeklyBookings.size}"
                )
                val monthBookings = bookingRepository.getMonthBookings()
                val todayProfit = bookingRepository.getTodayProfit()
                val monthProfit = bookingRepository.getMonthProfit()

                // Obter a data e hora atuais para filtrar os agendamentos passados
                val currentCalendar = Calendar.getInstance()
                val currentDate = currentCalendar.time
                val currentHour = currentCalendar.get(Calendar.HOUR_OF_DAY)

                // Filtrar agendamentos que ainda não passaram da data e horário
                val futureBookings = weeklyBookings.filter { booking ->
                    val bookingDate =
                        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).parse(booking.date)
                    if (bookingDate != null) {
                        // Comparar data e hora do agendamento com a data e hora atuais
                        bookingDate.after(currentDate) ||
                                (bookingDate == currentDate && booking.hour != null && booking.hour >= currentHour)
                    } else {
                        false
                    }
                }
                    .sortedWith(compareBy({ it.date }, { it.hour })) // Ordenar por data e hora
                    .take(2) // Pegar os 2 primeiros agendamentos futuros

                withContext(Dispatchers.Main) {
                    adminViewModel.setTodayBookingsCount(futureBookings.size)
                    adminViewModel.setMonthBookingsCount(monthBookings.size)
                    adminViewModel.setTodayProfit(todayProfit)
                    adminViewModel.setMonthProfit(monthProfit)

                    if (futureBookings.isEmpty()) {
                        noBookingsMessage.visibility = View.VISIBLE
                        noBookingsImage.visibility = View.VISIBLE
                        todayBookingsRecyclerView.visibility = View.GONE
                    } else {
                        noBookingsMessage.visibility = View.GONE
                        noBookingsImage.visibility = View.GONE
                        todayBookingsRecyclerView.visibility = View.VISIBLE
                        todayBookingsRecyclerView.adapter =
                            BookingAdapter(futureBookings, { bookingId ->
                                showConfirmationDialog(bookingId) // Diálogo para confirmar
                            }, { bookingId ->
                                showCancelConfirmationDialog(bookingId) // Diálogo para cancelar
                            })
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    noBookingsMessage.text = "Erro ao carregar dados."
                    noBookingsMessage.visibility = View.VISIBLE
                    noBookingsImage.visibility = View.VISIBLE
                    todayBookingsRecyclerView.visibility = View.GONE
                    Log.e("AdminHomeFragment", "Erro ao carregar dados: ", e)
                }
            }
        }
    }

    private fun showConfirmationDialog(bookingId: String) {
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Confirmar Agendamento")
            .setMessage("Você tem certeza que deseja confirmar este agendamento?")
            .setPositiveButton("Sim") { _, _ ->
                confirmBooking(bookingId) // Chama o método de confirmação
            }
            .setNegativeButton("Não", null)
            .create()

        dialog.show()
    }


    private fun confirmBooking(bookingId: String) {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("bookings").document(bookingId)
            .update("status", "confirmed")
            .addOnSuccessListener {
                Log.d("AdminHomeFragment", "Agendamento confirmado com sucesso!")
                loadData() // Atualiza os dados
                // Aqui você pode adicionar lógica para ocultar o botão ou agendamento
            }
            .addOnFailureListener { e ->
                Log.w("AdminHomeFragment", "Erro ao confirmar o agendamento: ", e)
            }
    }


    private fun showCancelConfirmationDialog(bookingId: String) {
        val dialog = AlertDialog.Builder(requireContext())
            .setTitle("Confirmar Cancelamento")
            .setMessage("Você tem certeza que deseja cancelar este agendamento?")
            .setPositiveButton("Sim") { _, _ ->
                cancelBooking(bookingId) // Chama o método de cancelamento
            }
            .setNegativeButton("Não", null)
            .create()

        dialog.show()
    }

    // Método para cancelar o agendamento
    private fun cancelBooking(bookingId: String) {
        val firestore = FirebaseFirestore.getInstance()
        firestore.collection("bookings").document(bookingId)
            .delete()
            .addOnSuccessListener {
                Log.d("AdminHomeFragment", "Agendamento cancelado com sucesso!")
                loadData() // Atualiza os dados
            }
            .addOnFailureListener { e ->
                Log.w("AdminHomeFragment", "Erro ao cancelar o agendamento: ", e)
            }
    }

    private fun loadProfileImage(imageUrl: String?) {
        val userImageView = view?.findViewById<CircleImageView>(R.id.userImage)
        imageUrl?.let {
            if (userImageView != null) {
                Glide.with(this)
                    .load(it) // URL da imagem no Firebase Storage
                    .placeholder(R.drawable.foto_perfil_generica) // Imagem de carregamento
                    .error(R.drawable.foto_perfil_generica) // Imagem em caso de erro
                    .into(userImageView)
            }
        } ?: run {
            userImageView?.setImageResource(R.drawable.foto_perfil_generica) // Ou uma imagem padrão se a URL for nula
        }
    }


    private fun loadBusinessName() {
        firestore = FirebaseFirestore.getInstance()
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid ?: run {
            userNameTextView.text = "Usuário não autenticado"
            return
        }

        firestore.collection("business").document(currentUserUid)
            .get()
            .addOnSuccessListener { document ->
                if (document != null) {
                    val business = document.toObject(Business::class.java)
                    adminViewModel.setBusinessName(business?.name ?: "Nome não disponível")
                    loadProfileImage(business?.imageUrl) // Carregar a imagem de perfil
                } else {
                    adminViewModel.setBusinessName("Nome não disponível")
                }
            }
            .addOnFailureListener { e ->
                adminViewModel.setBusinessName("Erro ao carregar o nome")
                Log.e("AdminHomeFragment", "Erro ao buscar o nome do negócio: ", e)
            }
    }

}
