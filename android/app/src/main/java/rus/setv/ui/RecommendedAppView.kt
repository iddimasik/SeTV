package rus.setv.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.Glide
import rus.setv.R
import rus.setv.model.AppItem

class RecommendedAppView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val image: ImageView
    private val title: TextView
    private val description: TextView
    private val badgeRecommended: TextView

    private var boundApp: AppItem? = null

    init {
        isFocusable = true
        isFocusableInTouchMode = true

        LayoutInflater.from(context)
            .inflate(R.layout.view_recommended_app, this, true)

        image = findViewById(R.id.recommendedImage)
        title = findViewById(R.id.recommendedTitle)
        description = findViewById(R.id.recommendedDescription)
        badgeRecommended = findViewById(R.id.badgeRecommended)

        // ───────────────────────
        // TV-ФОКУС (увеличение карточки)
        // ───────────────────────
        setOnFocusChangeListener { _, hasFocus ->
            animate()
                .scaleX(if (hasFocus) 1.06f else 1f)
                .scaleY(if (hasFocus) 1.06f else 1f)
                .setDuration(150)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()
        }

        // ───────────────────────
        // CLICK
        // ───────────────────────
        setOnClickListener {
            boundApp?.let { app ->
                onAppClick?.invoke(app)
            }
        }
    }

    /**
     * Привязка данных приложения к карточке
     */
    fun bind(app: AppItem) {
        boundApp = app

        title.text = app.name
        description.text = app.description ?: ""

        // показываем бэйдж только если приложение featured
        badgeRecommended.visibility = if (app.featured) View.VISIBLE else View.GONE

        // 🔥 ЗАГРУЗКА ИКОНКИ С СЕРВЕРА
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

    // callback на клик по приложению
    var onAppClick: ((AppItem) -> Unit)? = null
}
