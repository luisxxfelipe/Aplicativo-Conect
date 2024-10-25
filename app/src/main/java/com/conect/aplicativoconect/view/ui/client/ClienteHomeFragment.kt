package com.conect.aplicativoconect.view.ui.client

import CategoriesPagerAdapter
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.activityViewModels
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.databinding.FragmentClienteHomeBinding
import com.conect.aplicativoconect.view.data.model.Booking
import com.conect.aplicativoconect.view.data.model.Business
import com.conect.aplicativoconect.view.ui.admin.BusinessAdapter
import com.conect.aplicativoconect.view.viewmodel.ClientViewModel
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import java.util.Calendar

class ClienteHomeFragment : Fragment() {

    private var _binding: FragmentClienteHomeBinding? = null
    private val binding get() = _binding!!
    private lateinit var firestore: FirebaseFirestore
    private lateinit var businessAdapter: BusinessAdapter
    private val businessList = mutableListOf<Business>()
    private val clientViewModel: ClientViewModel by activityViewModels()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentClienteHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        firestore = FirebaseFirestore.getInstance()

        val userId = FirebaseAuth.getInstance().currentUser?.uid
        userId?.let {
            clientViewModel.loadUserData(it)
        }

        clientViewModel.userName.observe(viewLifecycleOwner) { userName ->
            binding.userName.text = userName ?: "Nome do Usuário"
            updateGreeting()
        }

        clientViewModel.userImage.observe(viewLifecycleOwner) { imageUrl ->
            Log.d("ClienteHomeFragment", "Loading image from URL: $imageUrl")
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.foto_perfil_generica)
                .error(R.drawable.foto_perfil_generica)
                .into(binding.userImage)
        }

        val categories = listOf("Manicure", "Barbearia", "Cabeleireiro", "Massagista", "Maquiagens")
        val categoriesPagerAdapter = CategoriesPagerAdapter(categories)

        binding.categoriesRecyclerView.layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
        binding.categoriesRecyclerView.adapter = categoriesPagerAdapter

        // Inicializando o adapter do business
        businessAdapter = BusinessAdapter(requireContext(), businessList) { business ->
            fetchBusinessIdAndOpenDetails(business.name) // Passa o nome da empresa
        }

        binding.establishmentsRecyclerView.layoutManager = LinearLayoutManager(requireContext())
        binding.establishmentsRecyclerView.adapter = businessAdapter

        fetchBusinesses()

        // Busca os agendamentos usando o índice "User_agendamentos"
        userId?.let { fetchUserBookings(it) }
    }

    private fun fetchUserBookings(userId: String) {
        Log.d("ClienteHomeFragment", "Buscando agendamentos para o usuário com ID: $userId")

        firestore.collection("bookings")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (_binding == null || !isAdded) return@addOnSuccessListener

                Log.d("ClienteHomeFragment", "Número de agendamentos encontrados: ${querySnapshot.size()}")

                val bookings = querySnapshot.documents.mapNotNull { document ->
                    val booking = document.toObject(Booking::class.java)
                    booking?.id = document.id // Atribui o ID do documento ao objeto Booking
                    booking
                }.filter { booking ->
                    isFutureBooking(booking) // Filtra agendamentos futuros
                }

                if (bookings.isEmpty()) {
                    binding.noBookingsMessage.visibility = View.VISIBLE
                    binding.noBookingsImage.visibility = View.VISIBLE
                    binding.todayBookingsRecyclerView.visibility = View.GONE
                } else {
                    binding.noBookingsMessage.visibility = View.GONE
                    binding.noBookingsImage.visibility = View.GONE
                    binding.todayBookingsRecyclerView.visibility = View.VISIBLE

                    val sortedBookings = bookings.sortedBy { it.hour }
                    val displayedBookings = sortedBookings.take(2)

                    binding.todayBookingsRecyclerView.layoutManager =
                        LinearLayoutManager(requireContext())
                    val adapter = ClientBookingAdapter(displayedBookings) { booking ->
                        cancelBooking(booking)
                    }
                    binding.todayBookingsRecyclerView.adapter = adapter
                }
            }
            .addOnFailureListener { e ->
                if (_binding == null || !isAdded) return@addOnFailureListener
                Log.e("ClienteHomeFragment", "Erro ao buscar agendamentos: ${e.message}")
                Toast.makeText(requireContext(), "Erro ao buscar agendamentos.", Toast.LENGTH_SHORT).show()
            }
    }


    private fun cancelBooking(booking: Booking) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("Cancelar Agendamento")
        builder.setMessage("Você tem certeza que deseja cancelar este agendamento?")

        builder.setPositiveButton("Sim") { dialog, _ ->
            val bookingId = booking.id
            Log.d(
                "ClienteHomeFragment",
                "Tentando apagar agendamento com ID: $bookingId"
            ) // Verifique o ID

            bookingId?.let {
                firestore.collection("bookings").document(it)
                    .delete()
                    .addOnSuccessListener {
                        Toast.makeText(
                            requireContext(),
                            "Agendamento cancelado com sucesso.",
                            Toast.LENGTH_SHORT
                        ).show()
                        // Atualizar a lista de agendamentos após o cancelamento
                        fetchUserBookings(FirebaseAuth.getInstance().currentUser?.uid ?: "")
                    }
                    .addOnFailureListener { e ->
                        Log.e("ClienteHomeFragment", "Erro ao cancelar agendamento: ${e.message}")
                        Toast.makeText(
                            requireContext(),
                            "Erro ao cancelar agendamento.",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
            } ?: run {
                Log.e("ClienteHomeFragment", "ID do agendamento é nulo. Não foi possível cancelar.")
                Toast.makeText(
                    requireContext(),
                    "Erro: ID do agendamento inválido.",
                    Toast.LENGTH_SHORT
                ).show()
            }
            dialog.dismiss()
        }

        builder.setNegativeButton("Não") { dialog, _ -> dialog.dismiss() }

        builder.show()
    }


    private fun isFutureBooking(booking: Booking): Boolean {
        val currentDateTime = Calendar.getInstance()
        val bookingDateParts = booking.date?.split("/")?.map { it.toInt() }
        if (bookingDateParts != null && bookingDateParts.size == 3) {
            val bookingCalendar = Calendar.getInstance().apply {
                set(Calendar.YEAR, bookingDateParts[2]) // YYYY
                set(Calendar.MONTH, bookingDateParts[1] - 1) // MM (0-11)
                set(Calendar.DAY_OF_MONTH, bookingDateParts[0]) // DD
                set(Calendar.HOUR_OF_DAY, booking.hour ?: 0) // HORA
                set(Calendar.MINUTE, 0) // MINUTO
            }
            return bookingCalendar.after(currentDateTime)
        }
        return false
    }

    private fun updateGreeting() {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
        val greeting = when {
            hour < 6 -> "Boa madrugada!"
            hour < 12 -> "Bom dia!"
            hour < 18 -> "Boa tarde!"
            else -> "Boa noite!"
        }
        binding.greetingTextView.text = greeting
    }

    private fun fetchBusinesses() {
        firestore.collection("business")
            .get()
            .addOnSuccessListener { querySnapshot ->
                if (_binding == null) return@addOnSuccessListener

                Log.d("HomeFragment", "Empresas encontradas: ${querySnapshot.size()}")
                if (!querySnapshot.isEmpty) {
                    businessList.clear()
                    for (document in querySnapshot.documents) {
                        val business = document.toObject(Business::class.java)
                        business?.let { businessList.add(it) }
                    }
                    businessAdapter.notifyDataSetChanged() // Notifica o adapter sobre as mudanças
                    binding.noEstablishmentsMessage.visibility = View.GONE
                    binding.establishmentsRecyclerView.visibility = View.VISIBLE
                } else {
                    Log.d("HomeFragment", "Nenhuma empresa encontrada.")
                    binding.noEstablishmentsMessage.visibility = View.VISIBLE
                    binding.establishmentsRecyclerView.visibility = View.GONE
                }
            }
            .addOnFailureListener { e ->
                if (_binding == null) return@addOnFailureListener

                Log.e("HomeFragment", "Erro ao buscar empresas", e)
                Toast.makeText(requireContext(), "Erro ao buscar empresas: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    private fun fetchBusinessIdAndOpenDetails(businessName: String) {
        firestore.collection("business")
            .whereEqualTo("name", businessName)
            .get()
            .addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    for (document in documents) {
                        val businessId = document.id
                        val intent = Intent(requireContext(), EmpresaDetalhesActivity::class.java).apply {
                            putExtra("companyId", businessId)
                        }
                        startActivity(intent)
                        break
                    }
                } else {
                    Log.d("ClienteHomeFragment", "Nenhuma empresa correspondente encontrada")
                }
            }
            .addOnFailureListener { e ->
                Log.w("ClienteHomeFragment", "Erro ao buscar documentos: ", e)
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
