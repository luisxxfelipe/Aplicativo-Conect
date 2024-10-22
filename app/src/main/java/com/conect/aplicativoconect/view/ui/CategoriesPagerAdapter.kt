import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.conect.aplicativoconect.R
import kotlin.random.Random

class CategoriesPagerAdapter(private val serviceList: List<String>) : RecyclerView.Adapter<CategoriesPagerAdapter.CategoryViewHolder>() {

    private val categoryColors = listOf(
        R.color.colorCategory1,
        R.color.colorCategory2,
        R.color.colorCategory3
    )

    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val serviceTextView: TextView = itemView.findViewById(R.id.serviceTextView)
    }

    private var lastColor: Int? = null // Variável para armazenar a última cor utilizada

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_service, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        // Defina o texto da categoria
        holder.serviceTextView.text = serviceList[position]

        var randomColorResId: Int
        do {
            randomColorResId = categoryColors[Random.nextInt(categoryColors.size)]
        } while (randomColorResId == lastColor) // Garante que a cor não é a mesma que a anterior

        lastColor = randomColorResId // Atualiza a última cor utilizada

        // Crie um GradientDrawable para aplicar a cor aleatória
        val randomColor = ContextCompat.getColor(holder.itemView.context, randomColorResId)
        val backgroundDrawable = GradientDrawable()
        backgroundDrawable.setColor(randomColor)
        backgroundDrawable.cornerRadius = 16f // Ajuste o raio conforme necessário

        // Aplique o Drawable de fundo
        holder.itemView.background = backgroundDrawable
    }

    override fun getItemCount(): Int {
        return serviceList.size
    }
}
