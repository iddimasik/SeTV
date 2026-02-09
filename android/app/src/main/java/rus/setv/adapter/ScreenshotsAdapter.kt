package rus.setv.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import rus.setv.R
import rus.setv.model.AppImage

class ScreenshotsAdapter(
    private val onClick: (position: Int) -> Unit
) : RecyclerView.Adapter<ScreenshotsAdapter.ViewHolder>() {

    private val items = mutableListOf<AppImage>()

    fun submitList(list: List<AppImage>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    fun getItems(): List<AppImage> = items

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_screenshot, parent, false)
        return ViewHolder(view, onClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(
        view: View,
        private val onClick: (Int) -> Unit
    ) : RecyclerView.ViewHolder(view) {

        private val card: MaterialCardView = view.findViewById(R.id.root)
        private val image: ImageView = view.findViewById(R.id.screenshotImage)

        init {
            // 🔹 Клик — всегда берём актуальную позицию
            card.setOnClickListener {
                val pos = bindingAdapterPosition
                if (pos != RecyclerView.NO_POSITION) {
                    onClick(pos)
                }
            }

            // 🔹 TV-фокус
            card.setOnFocusChangeListener { _, hasFocus ->
                card.animate()
                    .scaleX(if (hasFocus) 1.08f else 1f)
                    .scaleY(if (hasFocus) 1.08f else 1f)
                    .setDuration(150)
                    .start()
            }
        }

        fun bind(item: AppImage) {
            Glide.with(image)
                .load(item.imageUrl)
                .placeholder(R.drawable.ic_app_placeholder)
                .error(R.drawable.ic_app_placeholder)
                .into(image)
        }
    }
}
