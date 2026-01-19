package rus.setv.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.leanback.widget.Presenter
import com.bumptech.glide.Glide
import rus.setv.R
import rus.setv.model.AppItem

class AppCardPresenter(
    private val onClick: (AppItem) -> Unit
) : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.lb_image_card_view, parent, false)

        view.isFocusable = true
        view.isFocusableInTouchMode = true

        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val app = item as AppItem
        val root = viewHolder.view

        val image = root.findViewById<ImageView>(R.id.main_image)
        val title = root.findViewById<TextView>(R.id.title_text)
        val category = root.findViewById<TextView>(R.id.category_text)
        val desc = root.findViewById<TextView>(R.id.content_text)
        val content = root.findViewById<View>(R.id.cardContent)

        title.text = app.name
        category.text = app.category
        desc.text = app.description

        // 🔥 ЗАГРУЗКА ИКОНКИ С СЕРВЕРА
        Glide.with(root)
            .load(app.iconUrl)
            .placeholder(R.drawable.ic_app_placeholder)
            .error(R.drawable.ic_app_placeholder)
            .into(image)

        // ───────────────────────
        // TV-ФОКУС
        // ───────────────────────
        root.setOnFocusChangeListener { _, hasFocus ->
            val scale = if (hasFocus) 1.08f else 1.0f
            val elevation = if (hasFocus) 24f else 0f

            content.animate()
                .scaleX(scale)
                .scaleY(scale)
                .setDuration(150)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()

            root.elevation = elevation
        }

        // ───────────────────────
        // CLICK
        // ───────────────────────
        root.setOnClickListener {
            onClick(app)
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        viewHolder.view.findViewById<ImageView>(R.id.main_image)
            .setImageDrawable(null)
    }
}
