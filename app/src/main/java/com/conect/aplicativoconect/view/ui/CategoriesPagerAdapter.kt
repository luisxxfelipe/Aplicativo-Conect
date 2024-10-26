import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.conect.aplicativoconect.R
import java.util.Random // Importação correta para Random

class CategoriesPagerAdapter(
    private val serviceList: List<String>,
    private val onCategoryClick: (String) -> Unit
) : RecyclerView.Adapter<CategoriesPagerAdapter.CategoryViewHolder>() {

    private val categoryColors = listOf(
        R.color.colorCategory1,
        R.color.colorCategory2,
        R.color.colorCategory3
    )

    private var selectedPosition: Int = RecyclerView.NO_POSITION // Nenhuma posição selecionada inicialmente
    private var lastColor: Int? = null // Armazena a última cor usada

    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val serviceTextView: TextView = itemView.findViewById(R.id.serviceTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_service, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        // Define o texto da categoria
        holder.serviceTextView.text = serviceList[position]

        // Escolhe uma cor aleatória para o item
        var randomColorResId: Int
        do {
            randomColorResId = categoryColors[Random().nextInt(categoryColors.size)] // Usando Random da Java Util
        } while (randomColorResId == lastColor) // Evita repetir a última cor

        lastColor = randomColorResId // Atualiza a última cor utilizada
        val randomColor = ContextCompat.getColor(holder.itemView.context, randomColorResId)

        // Cria um GradientDrawable com a cor escolhida
        val backgroundDrawable = GradientDrawable().apply {
            setColor(randomColor)
            cornerRadius = 16f // Raio dos cantos
        }

        // Verifica se o item é o selecionado e ajusta a aparência
        if (position == selectedPosition) {
            backgroundDrawable.setStroke(6, ContextCompat.getColor(holder.itemView.context, R.color.colorAccent)) // Borda de destaque
        } else {
            backgroundDrawable.setStroke(0, randomColor) // Sem borda
        }

        holder.itemView.background = backgroundDrawable

        // Define o clique no item
        holder.itemView.setOnClickListener {
            val previousPosition = selectedPosition
            selectedPosition = holder.adapterPosition

            // Atualiza a aparência do item anterior e do atual
            if (previousPosition != RecyclerView.NO_POSITION) {
                notifyItemChanged(previousPosition)
            }
            notifyItemChanged(selectedPosition)

            // Chama a função de clique
            onCategoryClick(serviceList[selectedPosition])
        }
    }

    override fun getItemCount(): Int = serviceList.size
}
