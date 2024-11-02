import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.conect.aplicativoconect.R
import java.util.Random

class CategoriesPagerAdapter(
    private val serviceList: List<String>,
    private val onCategoryClick: (String) -> Unit
) : RecyclerView.Adapter<CategoriesPagerAdapter.CategoryViewHolder>() {

    private val categoryColors = listOf(
        R.color.colorCategory1,
        R.color.colorCategory2,
        R.color.colorCategory3
    )

    private var selectedPosition: Int =
        RecyclerView.NO_POSITION // Nenhuma posição selecionada inicialmente
    private val itemColors = mutableMapOf<Int, Int>() // Mapa para armazenar cores por item
    private var lastUsedColor: Int? = null // Última cor usada para garantir que não repita

    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val serviceTextView: TextView = itemView.findViewById(R.id.serviceTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_service, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        holder.serviceTextView.text = serviceList[position]

        // Gera uma cor que não seja igual à última usada
        val colorResId = itemColors.getOrPut(position) {
            getNextColor()
        }
        val color = ContextCompat.getColor(holder.itemView.context, colorResId)

        // Cria o GradientDrawable para manter a cor original do item
        val backgroundDrawable = GradientDrawable().apply {
            setColor(color)
            cornerRadius = 16f // Ajuste do raio
        }

        // Adiciona borda se o item estiver selecionado
        if (position == selectedPosition) {
            backgroundDrawable.setStroke(
                6,
                ContextCompat.getColor(holder.itemView.context, R.color.colorAccent)
            )
        } else {
            backgroundDrawable.setStroke(0, color) // Sem borda
        }

        holder.itemView.background = backgroundDrawable

        holder.itemView.setOnClickListener {
            val previousPosition = selectedPosition
            selectedPosition = holder.adapterPosition

            // Atualiza o item anterior e o atual
            if (previousPosition != RecyclerView.NO_POSITION) {
                notifyItemChanged(previousPosition)
            }
            notifyItemChanged(selectedPosition)

            // Chama a função de clique
            onCategoryClick(serviceList[selectedPosition])
        }
    }

    override fun getItemCount(): Int = serviceList.size

    // Método para obter a próxima cor que não repita a anterior
    private fun getNextColor(): Int {
        val availableColors = categoryColors.filter { it != lastUsedColor }
        val randomColor = availableColors[Random().nextInt(availableColors.size)]
        lastUsedColor = randomColor // Atualiza a última cor usada
        return randomColor
    }
}
