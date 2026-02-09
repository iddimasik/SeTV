package rus.setv.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import rus.setv.R

class FullscreenScreenshotsAdapter(
    private val images: List<String>,
    private val onClick: () -> Unit
) : RecyclerView.Adapter<FullscreenScreenshotsAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_screenshot_fullscreen, parent, false)
        return ViewHolder(view, onClick)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(images[position])
    }

    override fun getItemCount(): Int = images.size

    class ViewHolder(
        view: View,
        onClick: () -> Unit
    ) : RecyclerView.ViewHolder(view) {

        private val image: ImageView = view.findViewById(R.id.image)

        init {
            image.setOnClickListener { onClick() }
        }

        fun bind(url: String) {
            Glide.with(image)
                .load(url)
                .into(image)
        }
    }
}
