package rus.setv

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.viewpager2.widget.ViewPager2
import rus.setv.adapter.FullscreenScreenshotsAdapter

class ScreenshotViewerFragment :
    Fragment(R.layout.fragment_screenshot_viewer) {

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val images = requireArguments().getStringArrayList(ARG_IMAGES)!!
        val startPosition = requireArguments().getInt(ARG_POS)

        val pager = view.findViewById<ViewPager2>(R.id.pager)

        pager.adapter = FullscreenScreenshotsAdapter(images) {
            parentFragmentManager.popBackStack()
        }

        pager.setCurrentItem(startPosition, false)
    }

    companion object {
        private const val ARG_IMAGES = "images"
        private const val ARG_POS = "pos"

        fun newInstance(images: List<String>, startPosition: Int) =
            ScreenshotViewerFragment().apply {
                arguments = Bundle().apply {
                    putStringArrayList(ARG_IMAGES, ArrayList(images))
                    putInt(ARG_POS, startPosition)
                }
            }
    }
}
