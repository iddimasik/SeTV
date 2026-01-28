package rus.setv.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import com.google.android.material.card.MaterialCardView
import rus.setv.R
import rus.setv.model.BannerItem

class BannerCarousel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val cardContainer: MaterialCardView
    private val imageView: ImageView

    private var banners: List<BannerItem> = emptyList()
    private var index = 0

    private val switchDelay = 30000L
    private val animDuration = 300L

    var onBannerClick: ((BannerItem) -> Unit)? = null

    private val runnable = object : Runnable {
        override fun run() {
            if (banners.size < 2) return
            val next = (index + 1) % banners.size

            val fadeOut = ObjectAnimator.ofFloat(imageView, "alpha", 1f, 0f)
            fadeOut.duration = animDuration
            fadeOut.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    imageView.setImageResource(banners[next].imageRes)
                    ObjectAnimator.ofFloat(imageView, "alpha", 0f, 1f)
                        .setDuration(animDuration)
                        .start()
                    index = next
                }
            })
            fadeOut.start()
            postDelayed(this, switchDelay)
        }
    }

    init {
        LayoutInflater.from(context).inflate(R.layout.view_banner_carousel, this, true)

        val root = findViewById<FrameLayout>(R.id.bannerRoot)
        cardContainer = findViewById(R.id.cardContainer)
        imageView = findViewById(R.id.bannerImage)

        root.setOnFocusChangeListener { _, hasFocus ->
            val scale = if (hasFocus) 1.06f else 1.0f
            val elevation = if (hasFocus) 24f else 0f

            cardContainer.animate()
                .scaleX(scale)
                .scaleY(scale)
                .setDuration(150)
                .setInterpolator(android.view.animation.DecelerateInterpolator())
                .start()

            root.elevation = elevation
        }

        root.setOnClickListener {
            banners.getOrNull(index)?.let { banner ->
                onBannerClick?.invoke(banner)
            }
        }
    }

    fun setBanners(list: List<BannerItem>) {
        removeCallbacks(runnable)
        banners = list
        index = 0
        if (banners.isNotEmpty()) {
            imageView.setImageResource(banners[0].imageRes)
            imageView.alpha = 1f
            postDelayed(runnable, switchDelay)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        removeCallbacks(runnable)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = (width * 9) / 16
        val newHeightSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
        super.onMeasure(widthMeasureSpec, newHeightSpec)
    }
}