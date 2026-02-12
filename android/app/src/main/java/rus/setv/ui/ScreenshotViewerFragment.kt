package rus.setv

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide

class ScreenshotViewerFragment : Fragment(R.layout.fragment_screenshot_viewer) {

    private lateinit var imageView: ImageView
    private var images: List<String> = emptyList()
    private var currentPosition: Int = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        images = arguments?.getStringArrayList(ARG_IMAGES) ?: emptyList()
        currentPosition = arguments?.getInt(ARG_START_POSITION, 0) ?: 0

        imageView = view.findViewById(R.id.screenshotImage)

        // Make image view focusable
        imageView.isFocusable = true
        imageView.isFocusableInTouchMode = true

        loadImage(currentPosition)

        // Handle key events
        view.setOnKeyListener { _, keyCode, event ->
            when {
                (keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                        keyCode == KeyEvent.KEYCODE_ENTER ||
                        keyCode == KeyEvent.KEYCODE_BACK) &&
                        event.action == KeyEvent.ACTION_DOWN -> {
                    // Close viewer
                    parentFragmentManager.popBackStack()
                    true
                }
                keyCode == KeyEvent.KEYCODE_DPAD_RIGHT &&
                        event.action == KeyEvent.ACTION_DOWN -> {
                    // Next image
                    if (currentPosition < images.size - 1) {
                        currentPosition++
                        loadImage(currentPosition)
                    }
                    true
                }
                keyCode == KeyEvent.KEYCODE_DPAD_LEFT &&
                        event.action == KeyEvent.ACTION_DOWN -> {
                    // Previous image
                    if (currentPosition > 0) {
                        currentPosition--
                        loadImage(currentPosition)
                    }
                    true
                }
                else -> false
            }
        }

        // Also add key listener to imageView itself for redundancy
        imageView.setOnKeyListener { _, keyCode, event ->
            when {
                (keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                        keyCode == KeyEvent.KEYCODE_ENTER ||
                        keyCode == KeyEvent.KEYCODE_BACK) &&
                        event.action == KeyEvent.ACTION_DOWN -> {
                    parentFragmentManager.popBackStack()
                    true
                }
                keyCode == KeyEvent.KEYCODE_DPAD_RIGHT &&
                        event.action == KeyEvent.ACTION_DOWN -> {
                    if (currentPosition < images.size - 1) {
                        currentPosition++
                        loadImage(currentPosition)
                    }
                    true
                }
                keyCode == KeyEvent.KEYCODE_DPAD_LEFT &&
                        event.action == KeyEvent.ACTION_DOWN -> {
                    if (currentPosition > 0) {
                        currentPosition--
                        loadImage(currentPosition)
                    }
                    true
                }
                else -> false
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Focus imageView immediately on resume
        imageView.post {
            imageView.requestFocus()
        }
    }

    private fun loadImage(position: Int) {
        if (position >= 0 && position < images.size) {
            Glide.with(this)
                .load(images[position])
                .into(imageView)
        }
    }

    companion object {
        private const val ARG_IMAGES = "images"
        private const val ARG_START_POSITION = "start_position"

        fun newInstance(images: List<String>, startPosition: Int): ScreenshotViewerFragment {
            return ScreenshotViewerFragment().apply {
                arguments = Bundle().apply {
                    putStringArrayList(ARG_IMAGES, ArrayList(images))
                    putInt(ARG_START_POSITION, startPosition)
                }
            }
        }
    }
}
