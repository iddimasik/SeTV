package rus.setv.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import rus.setv.R
import rus.setv.model.AppItem

class RecommendedAppView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val image: ImageView
    private val title: TextView
    private val category: TextView
    private val badgeRecommended: TextView
    private val cardContent: MaterialCardView

    private var boundApp: AppItem? = null

    init {
        LayoutInflater.from(context)
            .inflate(R.layout.view_recommended_app, this, true)

        val root = findViewById<FrameLayout>(R.id.recommendedRoot)
        cardContent = findViewById(R.id.cardContent)
        image = findViewById(R.id.recommendedImage)
        title = findViewById(R.id.recommendedTitle)
        category = findViewById(R.id.recommendedCategory)
        badgeRecommended = findViewById(R.id.badgeRecommended)

        root.setOnFocusChangeListener { _, hasFocus ->
            val scale = if (hasFocus) 1.08f else 1f
            val elevation = if (hasFocus) 24f else 0f

            cardContent.animate()
                .scaleX(scale)
                .scaleY(scale)
                .setDuration(150)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()

            root.elevation = elevation
        }

        root.setOnClickListener {
            boundApp?.let { onAppClick?.invoke(it) }
        }
    }

    fun bind(app: AppItem) {
        boundApp = app

        title.text = app.name
        category.text = app.category ?: ""

        badgeRecommended.visibility =
            if (app.featured) VISIBLE else GONE

        Glide.with(this)
            .load(app.iconUrl)
            .placeholder(R.drawable.ic_app_placeholder)
            .error(R.drawable.ic_app_placeholder)
            .into(image)
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        Glide.with(this).clear(image)
    }

    var onAppClick: ((AppItem) -> Unit)? = null
}