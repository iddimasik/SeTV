package rus.setv.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import com.google.android.material.card.MaterialCardView
import rus.setv.R
import rus.setv.model.BannerItem

class BannerCarousel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val rootContainer: FrameLayout
    private val cardContainer: MaterialCardView
    private val imageView: ImageView

    private var banners: List<BannerItem> = emptyList()
    private var index = 0

    private val switchDelay = 30000L
    private val animDuration = 300L

    var onBannerClick: ((BannerItem) -> Unit)? = null
    var onLeftKey: (() -> Unit)? = null

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
        LayoutInflater.from(context)
            .inflate(R.layout.view_banner_carousel, this, true)

        rootContainer = findViewById(R.id.bannerRoot)
        cardContainer = findViewById(R.id.cardContainer)
        imageView = findViewById(R.id.bannerImage)

        rootContainer.setOnFocusChangeListener { _, hasFocus ->

            val scale = if (hasFocus) 1.05f else 1f
            val elevation = if (hasFocus) 24f else 0f

            cardContainer.animate()
                .scaleX(scale)
                .scaleY(scale)
                .setDuration(150)
                .setInterpolator(DecelerateInterpolator())
                .start()

            cardContainer.strokeWidth = if (hasFocus) {
                (3 * resources.displayMetrics.density).toInt()
            } else {
                0
            }

            rootContainer.elevation = elevation
        }

        rootContainer.setOnClickListener {
            banners.getOrNull(index)?.let {
                onBannerClick?.invoke(it)
            }
        }

        rootContainer.setOnKeyListener { _, keyCode, event ->
            if (keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT &&
                event.action == android.view.KeyEvent.ACTION_DOWN) {
                onLeftKey?.invoke()
                true
            } else false
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
