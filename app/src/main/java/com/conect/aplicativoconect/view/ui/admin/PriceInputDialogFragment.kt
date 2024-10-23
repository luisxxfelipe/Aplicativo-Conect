import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.EditText
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.fragment.app.DialogFragment
import com.conect.aplicativoconect.R
import com.conect.aplicativoconect.view.data.model.ServiceType

class PriceInputDialogFragment : DialogFragment() {
    private lateinit var serviceType: ServiceType
    private lateinit var priceInput: EditText

    fun setServiceType(serviceType: ServiceType) {
        this.serviceType = serviceType
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext())
        val inflater = requireActivity().layoutInflater
        val dialogView = inflater.inflate(R.layout.dialog_price_input, null)

        priceInput = dialogView.findViewById(R.id.editTextPrice)

        builder.setView(dialogView)
            .setTitle("Insira o preço para ${serviceType.name}")
            .setPositiveButton("OK") { _, _ ->
                val price = priceInput.text.toString().toDoubleOrNull()
                if (price != null) {
                    // Aqui você deve salvar o preço para o serviço
                    saveServiceWithPrice(serviceType, price)
                }
            }
            .setNegativeButton("Cancelar", null)

        return builder.create()
    }

    private fun saveServiceWithPrice(serviceType: ServiceType, price: Double) {
        // Lógica para salvar o serviço e o preço no Firestore ou onde for necessário
    }
}
