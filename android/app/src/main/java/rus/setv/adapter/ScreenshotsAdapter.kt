package rus.setv.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import rus.setv.R
import rus.setv.model.AppImage

class ScreenshotsAdapter :
    ListAdapter<AppImage, ScreenshotsAdapter.ViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_screenshot, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val image: ImageView = itemView.findViewById(R.id.screenshotImage)

        fun bind(item: AppImage) {
            Glide.with(itemView)
                .load(item.imageUrl)
                .placeholder(R.drawable.ic_app_placeholder)
                .error(R.drawable.ic_app_placeholder)
                .into(image)
        }
    }

    private class DiffCallback : DiffUtil.ItemCallback<AppImage>() {
        override fun areItemsTheSame(old: AppImage, new: AppImage): Boolean =
            old.imageUrl == new.imageUrl

        override fun areContentsTheSame(old: AppImage, new: AppImage): Boolean =
            old == new
    }
}
