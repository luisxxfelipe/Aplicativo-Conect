import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.databinding.FragmentAdminProfileBinding
import com.conect.aplicativoconect.view.ui.admin.OperatingHoursFragment

class AdminProfileFragment : Fragment() {

    private var _binding: FragmentAdminProfileBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAdminProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Listener para abrir o fragmento de horários de funcionamento
        binding.hoursButtonAdmin.setOnClickListener {
            openOperatingHoursFragment()
        }
    }

    private fun openOperatingHoursFragment() {
        // Troca o fragmento atual pelo OperatingHoursFragment
        parentFragmentManager.beginTransaction()
            .replace(R.id.fragment_container, OperatingHoursFragment()) // Certifique-se de que o ID 'fragment_container' exista na activity que carrega os fragmentos
            .addToBackStack(null)
            .commit()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
