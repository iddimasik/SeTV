package rus.setv.ui

import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.leanback.widget.Presenter
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import rus.setv.R
import rus.setv.model.AppItem
import rus.setv.model.AppStatus

class AppCardPresenter(
    private val onClick: (AppItem) -> Unit
) : Presenter() {

    var onFirstRowNavigateUp: (() -> Unit)? = null
    var isFirstRowProvider: (() -> Boolean)? = null
    var onNavigateLeft: (() -> Unit)? = null
    var isFirstColumnProvider: (() -> Boolean)? = null

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.view_app, parent, false)

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
        val content = root.findViewById<MaterialCardView>(R.id.cardContent)
        val badge = root.findViewById<ImageView>(R.id.status_badge)

        title.text = app.name
        category.text = app.category
        desc.text = app.description

        // STATUS BADGE
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

        // ICON
        Glide.with(root)
            .load(app.iconUrl)
            .placeholder(R.drawable.ic_app_placeholder)
            .error(R.drawable.ic_app_placeholder)
            .into(image)

        root.setOnFocusChangeListener { _, hasFocus ->

            val scale = if (hasFocus) 1.05f else 1f
            val elevation = if (hasFocus) 20f else 0f

            root.animate()
                .scaleX(scale)
                .scaleY(scale)
                .setDuration(160)
                .setInterpolator(DecelerateInterpolator())
                .start()

            content.strokeWidth = if (hasFocus) {
                (2 * root.resources.displayMetrics.density).toInt()
            } else {
                0
            }

            root.elevation = elevation
        }

        root.setOnClickListener {
            onClick(app)
        }

        root.setOnKeyListener { _, keyCode, event ->
            when {
                keyCode == KeyEvent.KEYCODE_DPAD_UP &&
                        event.action == KeyEvent.ACTION_DOWN &&
                        isFirstRowProvider?.invoke() == true -> {
                    onFirstRowNavigateUp?.invoke()
                    true
                }

                keyCode == KeyEvent.KEYCODE_DPAD_LEFT &&
                        event.action == KeyEvent.ACTION_DOWN &&
                        isFirstColumnProvider?.invoke() == true -> {
                    onNavigateLeft?.invoke()
                    true
                }

                else -> false
            }
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        viewHolder.view.findViewById<ImageView>(R.id.main_image)
            .setImageDrawable(null)
    }
}