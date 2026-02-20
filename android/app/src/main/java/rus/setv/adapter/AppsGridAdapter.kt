package rus.setv.ui

import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import rus.setv.R
import rus.setv.model.AppItem
import rus.setv.model.AppStatus
import androidx.core.graphics.toColorInt

class AppsGridAdapter(
    private val onClick: (AppItem) -> Unit
) : RecyclerView.Adapter<AppsGridAdapter.AppViewHolder>() {

    private val apps = mutableListOf<AppItem>()

    var onFirstRowNavigateUp: ((Int) -> Unit)? = null
    var onNavigateLeft: ((Int) -> Unit)? = null
    var onFocusPositionChanged: ((Int) -> Unit)? = null

    fun setApps(newApps: List<AppItem>) {
        apps.clear()
        apps.addAll(newApps)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.view_app, parent, false)

        val density = parent.context.resources.displayMetrics.density
        val layoutParams = view.layoutParams
        layoutParams.height = (145 * density).toInt()
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        view.layoutParams = layoutParams

        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        holder.bind(apps[position], position)
    }

    override fun getItemCount() = apps.size

    inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val image: ImageView = itemView.findViewById(R.id.main_image)
        private val title: TextView = itemView.findViewById(R.id.title_text)
        private val category: TextView = itemView.findViewById(R.id.category_text)
        private val desc: TextView = itemView.findViewById(R.id.content_text)
        private val content: MaterialCardView = itemView.findViewById(R.id.cardContent)
        private val badge: ImageView = itemView.findViewById(R.id.status_badge)

        init {
            // 🔥 ВАЖНО — фокус на itemView, не на content!
            itemView.isFocusable = true
            itemView.isFocusableInTouchMode = true

            content.isFocusable = false
            content.isFocusableInTouchMode = false
            content.isClickable = false

            // Set default stroke width, color, and background
            val density = itemView.resources.displayMetrics.density
            content.strokeWidth = (2 * density).toInt()
            content.strokeColor = "#80505050".toColorInt()
            content.setCardBackgroundColor("#99282828".toColorInt())  // Unfocused background

            itemView.setOnClickListener {
                bindingAdapterPosition.takeIf { it != RecyclerView.NO_POSITION }
                    ?.let { pos -> onClick(apps[pos]) }
            }

            itemView.setOnFocusChangeListener { _, hasFocus ->

                val position = bindingAdapterPosition
                if (position != RecyclerView.NO_POSITION && hasFocus) {
                    onFocusPositionChanged?.invoke(position)
                }

                // Change stroke width AND color on focus
                if (hasFocus) {
                    content.strokeWidth = (3 * density).toInt()
                    content.strokeColor = "#09E490".toColorInt()  // GREEN
                    content.setCardBackgroundColor("#80505050".toColorInt())  // Focused background
                } else {
                    content.strokeWidth = (2 * density).toInt()
                    content.strokeColor = "#80505050".toColorInt()  // White semi-transparent
                    content.setCardBackgroundColor("#99282828".toColorInt())  // Unfocused background
                }

                // Animate itemView (ROOT), not content - prevents clipping
                itemView.animate()
                    .scaleX(if (hasFocus) 1.06f else 1f)
                    .scaleY(if (hasFocus) 1.06f else 1f)
                    .setDuration(160)
                    .setInterpolator(DecelerateInterpolator())
                    .start()

                // Elevation on root
                itemView.elevation = if (hasFocus) 20f else 0f
            }

            itemView.setOnKeyListener { _, keyCode, event ->
                if (event.action != KeyEvent.ACTION_DOWN) return@setOnKeyListener false

                val position = bindingAdapterPosition
                if (position == RecyclerView.NO_POSITION) return@setOnKeyListener false

                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_UP -> {
                        if (position < 3) {
                            onFirstRowNavigateUp?.invoke(position)
                            true
                        } else false
                    }

                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        if (position % 3 == 0) {
                            onNavigateLeft?.invoke(position)
                            true
                        } else false
                    }

                    else -> false
                }
            }
        }

        fun bind(app: AppItem, position: Int) {

            title.text = app.name
            category.text = app.category
            desc.text = app.description

            val badgeRes = when (app.status) {
                AppStatus.INSTALLED -> R.drawable.ic_installed
                AppStatus.NOT_INSTALLED -> R.drawable.ic_uninstalled
                AppStatus.UPDATE_AVAILABLE -> R.drawable.ic_upgrade
                AppStatus.DOWNLOADING,
                AppStatus.INSTALLING,
                AppStatus.ERROR -> R.drawable.ic_programs
            }

            badge.setImageResource(badgeRes)
            badge.visibility = View.VISIBLE

            Glide.with(itemView)
                .load(app.iconUrl)
                .placeholder(R.drawable.ic_app_placeholder)
                .error(R.drawable.ic_app_placeholder)
                .into(image)

            // 🔥 СБРОС scale при реюзе на itemView
            itemView.scaleX = if (itemView.isFocused) 1.06f else 1f
            itemView.scaleY = if (itemView.isFocused) 1.06f else 1f
            itemView.elevation = if (itemView.isFocused) 20f else 0f
        }
    }
}