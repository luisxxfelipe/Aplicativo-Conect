import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.conect.aplicativoconect.R
import java.util.Random

class CategoriesPagerAdapter(
    private val serviceList: List<String>,
    private val onCategoryClick: (String?) -> Unit // Envia null para indicar que o filtro foi removido
) : RecyclerView.Adapter<CategoriesPagerAdapter.CategoryViewHolder>() {

    private val categoryColors = listOf(
        R.color.colorCategory1,
        R.color.colorCategory2,
        R.color.colorCategory3
    )

    private val categoryIcons = mapOf(
        "Cabeleireiro" to R.drawable.ic_cabeleireiro,
        "Manicure" to R.drawable.ic_manicure,
        "Estética" to R.drawable.ic_estetica,
        "Barbeiro" to R.drawable.ic_barbearia,
        "Massagem" to R.drawable.ic_massagem,
        "Técnico de Informática" to R.drawable.ic_ti,
        "Fotógrafo" to R.drawable.ic_fotografia,
        "Depilação" to R.drawable.ic_depilacao,
        "Desenvolvedor de sites" to R.drawable.ic_developer
    )


    private var selectedPosition: Int =
        RecyclerView.NO_POSITION // Nenhuma posição selecionada inicialmente
    private val itemColors = mutableMapOf<Int, Int>() // Mapa para armazenar cores por item
    private var lastUsedColor: Int? = null // Última cor usada para evitar repetição

    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val serviceTextView: TextView = itemView.findViewById(R.id.serviceTextView)
        val serviceIcon: ImageView = itemView.findViewById(R.id.serviceIcon)
        val background: View = itemView.findViewById(R.id.iconBackground) // Para o fundo redondo
    }


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_service, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        val category = serviceList[position]
        holder.serviceTextView.text = category

        // Atribui o ícone correspondente à categoria
        val iconResId = categoryIcons[category] ?: R.drawable.ic_service
        holder.serviceIcon.setImageResource(iconResId)

        // Gera uma cor que não seja igual à última usada
        val colorResId = itemColors.getOrPut(position) {
            getNextColor()
        }
        val color = ContextCompat.getColor(holder.itemView.context, colorResId)

        // Cria o GradientDrawable para manter a cor original do item
        val backgroundDrawable = GradientDrawable().apply {
            setColor(color)
            cornerRadius = 100f // Certifique-se de que o fundo é circular
        }

        // Adiciona borda se o item estiver selecionado
        if (position == selectedPosition) {
            backgroundDrawable.setStroke(
                6,
                ContextCompat.getColor(holder.itemView.context, R.color.colorAccent)
            )
        }

        holder.background.background = backgroundDrawable

        holder.itemView.setOnClickListener {
            val previousPosition = selectedPosition
            if (selectedPosition == position) {
                // Clique na mesma categoria: remove a seleção
                selectedPosition = RecyclerView.NO_POSITION
                onCategoryClick(null) // Envia null para indicar que o filtro foi removido
            } else {
                // Atualiza a seleção
                selectedPosition = holder.adapterPosition
                onCategoryClick(serviceList[selectedPosition]) // Envia a nova seleção
            }

            // Atualiza o item anterior e o atual
            if (previousPosition != RecyclerView.NO_POSITION) {
                notifyItemChanged(previousPosition)
            }
            notifyItemChanged(selectedPosition)
        }
    }


    override fun getItemCount(): Int = serviceList.size

    private var colorIndex = 0 // Índice inicial

    private fun getNextColor(): Int {
        val color = categoryColors[colorIndex]
        colorIndex = (colorIndex + 1) % categoryColors.size // Move para o próximo índice, voltando ao início se necessário
        return color
    }

}
