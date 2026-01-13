package rus.setv.ui

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.ImageView
import androidx.leanback.widget.RowPresenter
import rus.setv.R
import rus.setv.model.BannerRow

class BannerRowPresenter : RowPresenter() {

    private val switchDelay = 5000L
    private val animDuration = 300L

    init {
        setHeaderPresenter(null)
    }

    override fun createRowViewHolder(parent: ViewGroup): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.row_banner, parent, false)

        view.layoutParams = ViewGroup.LayoutParams(
            parent.resources.getDimensionPixelSize(R.dimen.banner_row_width),
            parent.resources.getDimensionPixelSize(R.dimen.banner_row_height)
        )

        return ViewHolder(view)
    }

    override fun onBindRowViewHolder(vh: ViewHolder, item: Any) {
        val row = item as BannerRow
        val image = vh.view.findViewById<ImageView>(R.id.bannerImage)

        if (row.banners.isEmpty()) return

        val images = row.banners.map { it.imageRes }

        image.setImageResource(images.first())
        image.alpha = 1f
        image.isFocusable = true
        image.isFocusableInTouchMode = true
        image.requestFocus()

        startCarousel(image, images)
    }

    override fun onUnbindRowViewHolder(vh: ViewHolder) {
        vh.view.findViewById<ImageView>(R.id.bannerImage)
            ?.removeCallbacks(null)
    }

    private fun startCarousel(
        imageView: ImageView,
        images: List<Int>
    ) {
        if (images.size < 2) return

        var index = 0

        val runnable = object : Runnable {
            override fun run() {
                val nextIndex = (index + 1) % images.size

                val fadeOut = ObjectAnimator.ofFloat(imageView, "alpha", 1f, 0f)
                fadeOut.duration = animDuration

                fadeOut.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        imageView.setImageResource(images[nextIndex])

                        val fadeIn =
                            ObjectAnimator.ofFloat(imageView, "alpha", 0f, 1f)
                        fadeIn.duration = animDuration
                        fadeIn.start()

                        index = nextIndex
                    }
                })

                fadeOut.start()
                imageView.postDelayed(this, switchDelay)
            }
        }

        imageView.postDelayed(runnable, switchDelay)
    }

    override fun isUsingDefaultSelectEffect(): Boolean = false
}