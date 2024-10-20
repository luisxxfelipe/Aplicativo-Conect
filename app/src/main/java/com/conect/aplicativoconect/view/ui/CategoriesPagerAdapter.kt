import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.conect.aplicativoconect.R

class CategoriesPagerAdapter(private val serviceList: List<String>) : RecyclerView.Adapter<CategoriesPagerAdapter.CategoryViewHolder>() {

    // Defina um array de cores ou pegue do colors.xml
    private val categoryColors = listOf(
        R.color.colorCategory1,
        R.color.colorCategory2,
        R.color.colorCategory3,
    )

    class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val serviceTextView: TextView = itemView.findViewById(R.id.serviceTextView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CategoryViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_service, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: CategoryViewHolder, position: Int) {
        // Defina o texto da categoria
        holder.serviceTextView.text = serviceList[position]

        // Aplique uma cor de fundo diferente para cada item, baseado na posição
        val colorIndex = position % categoryColors.size
        holder.itemView.setBackgroundColor(
            ContextCompat.getColor(holder.itemView.context, categoryColors[colorIndex])
        )
    }

    override fun getItemCount(): Int {
        return serviceList.size
    }
}
