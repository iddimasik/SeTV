package rus.setv.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import rus.setv.R
import rus.setv.model.BannerItem

class BannerCarousel @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : FrameLayout(context, attrs) {

    private val imageView: ImageView

    private var banners: List<BannerItem> = emptyList()
    private var index = 0

    private val switchDelay = 5000L
    private val animDuration = 300L

    // 🔥 callback клика
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
        isFocusable = true
        isFocusableInTouchMode = true

        LayoutInflater.from(context)
            .inflate(R.layout.view_banner_carousel, this, true)

        imageView = findViewById(R.id.bannerImage)

        // ───────── FOCUS EFFECT ─────────
        setOnFocusChangeListener { _, hasFocus ->
            animate()
                .scaleX(if (hasFocus) 1.05f else 1f)
                .scaleY(if (hasFocus) 1.05f else 1f)
                .setDuration(150)
                .start()
        }

        // ───────── CLICK ─────────
        setOnClickListener {
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
}
