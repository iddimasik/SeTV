package rus.setv.ui

import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import rus.setv.R
import rus.setv.model.AppItem
import rus.setv.model.AppStatus

class AppsGridAdapter(
    private val onClick: (AppItem) -> Unit
) : RecyclerView.Adapter<AppsGridAdapter.AppViewHolder>() {

    private val apps = mutableListOf<AppItem>()

    // Navigation callbacks
    var onFirstRowNavigateUp: ((Int) -> Unit)? = null
    var onNavigateLeft: ((Int) -> Unit)? = null

    fun setApps(newApps: List<AppItem>) {
        apps.clear()
        apps.addAll(newApps)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): AppViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.view_app, parent, false)

        // Set fixed compact height for card - matching original design
        val density = parent.context.resources.displayMetrics.density
        val layoutParams = view.layoutParams
        layoutParams.height = (125 * density).toInt() // 100dp height for compact cards
        layoutParams.width = ViewGroup.LayoutParams.MATCH_PARENT
        view.layoutParams = layoutParams

        return AppViewHolder(view)
    }

    override fun onBindViewHolder(holder: AppViewHolder, position: Int) {
        val app = apps[position]
        holder.bind(app, position)
    }

    override fun getItemCount() = apps.size

    inner class AppViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val image: ImageView = itemView.findViewById(R.id.main_image)
        private val title: TextView = itemView.findViewById(R.id.title_text)
        private val category: TextView = itemView.findViewById(R.id.category_text)
        private val desc: TextView = itemView.findViewById(R.id.content_text)
        private val content: com.google.android.material.card.MaterialCardView = itemView.findViewById(R.id.cardContent)
        private val badge: ImageView = itemView.findViewById(R.id.status_badge)

        fun bind(app: AppItem, position: Int) {
            title.text = app.name
            category.text = app.category
            desc.text = app.description

            // Set badge
            val badgeRes = when (app.status) {
                AppStatus.INSTALLED -> R.drawable.ic_installed
                AppStatus.NOT_INSTALLED -> R.drawable.ic_uninstalled
                AppStatus.UPDATE_AVAILABLE -> R.drawable.ic_upgrade
                AppStatus.DOWNLOADING -> R.drawable.ic_programs
                AppStatus.INSTALLING -> R.drawable.ic_programs
                AppStatus.ERROR -> R.drawable.ic_programs
            }
            badge.setImageResource(badgeRes)
            badge.visibility = View.VISIBLE

            Glide.with(itemView)
                .load(app.iconUrl)
                .placeholder(R.drawable.ic_app_placeholder)
                .error(R.drawable.ic_app_placeholder)
                .into(image)

            itemView.isFocusable = true
            itemView.isFocusableInTouchMode = true

            // Focus animation - EXACTLY like original
            itemView.setOnFocusChangeListener { _, hasFocus ->
                val scale = if (hasFocus) 1.05f else 1f
                val elevation = if (hasFocus) 20f else 0f

                // Animate the ROOT (itemView), not content
                itemView.animate()
                    .scaleX(scale)
                    .scaleY(scale)
                    .setDuration(160)
                    .setInterpolator(android.view.animation.DecelerateInterpolator())
                    .start()

                // Set stroke on MaterialCardView
                content.strokeWidth = if (hasFocus) {
                    (2 * itemView.resources.displayMetrics.density).toInt()
                } else {
                    0
                }

                itemView.elevation = elevation
            }

            itemView.setOnClickListener {
                onClick(app)
            }

            // Handle navigation
            itemView.setOnKeyListener { _, keyCode, event ->
                if (event.action != KeyEvent.ACTION_DOWN) {
                    return@setOnKeyListener false
                }

                when (keyCode) {
                    KeyEvent.KEYCODE_DPAD_UP -> {
                        if (position < 3) {
                            // First row - go to filters
                            onFirstRowNavigateUp?.invoke(position)
                            true
                        } else false
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        if (position % 3 == 0) {
                            // First column - open sidebar
                            onNavigateLeft?.invoke(position)
                            true
                        } else false
                    }
                    else -> false
                }
            }
        }
    }
}
