package rus.setv.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import com.google.android.material.card.MaterialCardView
import rus.setv.R
import rus.setv.model.AppItem
import androidx.core.graphics.toColorInt

class RecommendedAppView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val image: ImageView
    private val title: TextView
    private val category: TextView
    private val badgeRecommended: TextView
    private val cardContent: MaterialCardView
    private val rootContainer: FrameLayout

    private var boundApp: AppItem? = null

    var onAppClick: ((AppItem) -> Unit)? = null

    init {
        LayoutInflater.from(context)
            .inflate(R.layout.view_recommended_app, this, true)

        rootContainer = findViewById(R.id.recommendedRoot)
        cardContent = findViewById(R.id.cardContent)
        image = findViewById(R.id.recommendedImage)
        title = findViewById(R.id.recommendedTitle)
        category = findViewById(R.id.recommendedCategory)
        badgeRecommended = findViewById(R.id.badgeRecommended)

        // ⚠️ ВАЖНО: фокус только у root
        rootContainer.isFocusable = true
        rootContainer.isFocusableInTouchMode = true

        // Set default stroke width, color and background
        val density = context.resources.displayMetrics.density
        cardContent.strokeWidth = (1 * density).toInt()
        cardContent.strokeColor = "#80505050".toColorInt()  // White semi-transparent
        cardContent.setCardBackgroundColor("#99282828".toColorInt())  // Unfocused background

        rootContainer.setOnFocusChangeListener { _, hasFocus ->

            // Change stroke width, color AND background on focus
            if (hasFocus) {
                cardContent.strokeWidth = (2 * density).toInt()
                cardContent.strokeColor = "#09E490".toColorInt()  // Green
                cardContent.setCardBackgroundColor("#80505050".toColorInt())  // Focused background
            } else {
                cardContent.strokeWidth = (1 * density).toInt()
                cardContent.strokeColor = "#80505050".toColorInt()  // White semi-transparent
                cardContent.setCardBackgroundColor("#99282828".toColorInt())  // Unfocused background
            }

            // Мягкий TV-scale (не раздувает карточку)
            cardContent.animate()
                .scaleX(if (hasFocus) 1.05f else 1f)
                .scaleY(if (hasFocus) 1.05f else 1f)
                .setDuration(120)
                .setInterpolator(DecelerateInterpolator())
                .start()
        }

        rootContainer.setOnClickListener {
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
}