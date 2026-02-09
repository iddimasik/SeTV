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

class ScreenshotsAdapter :
    RecyclerView.Adapter<ScreenshotsAdapter.ViewHolder>() {

    private val items = mutableListOf<AppImage>()

    fun submitList(list: List<AppImage>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_screenshot, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        private val card: MaterialCardView = view.findViewById(R.id.root)
        private val image: ImageView = view.findViewById(R.id.screenshotImage)

        fun bind(item: AppImage) {
            Glide.with(image)
                .load(item.imageUrl)
                .placeholder(R.drawable.ic_app_placeholder)
                .into(image)

            card.setOnFocusChangeListener { _, hasFocus ->
                card.animate()
                    .scaleX(if (hasFocus) 1.08f else 1f)
                    .scaleY(if (hasFocus) 1.08f else 1f)
                    .setDuration(150)
                    .start()
            }
        }
    }
}
