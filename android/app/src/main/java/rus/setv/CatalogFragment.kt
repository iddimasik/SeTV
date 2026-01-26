package rus.setv

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HorizontalGridView
import androidx.leanback.widget.ItemBridgeAdapter
import androidx.leanback.widget.VerticalGridView
import kotlinx.coroutines.launch
import rus.setv.data.repository.AppsRepository
import rus.setv.model.AppItem
import rus.setv.model.BannerItem
import rus.setv.ui.AppCardPresenter
import rus.setv.ui.BannerCarousel
import rus.setv.ui.RecommendedAppPresenter

class CatalogFragment : Fragment(R.layout.fragment_catalog),
    MainActivity.SidebarListener {

    private lateinit var grid: VerticalGridView
    private lateinit var adapter: ArrayObjectAdapter
    private lateinit var bannerCarousel: BannerCarousel

    private lateinit var recommendedGrid: VerticalGridView
    private lateinit var recommendedAdapter: ArrayObjectAdapter

    private val repository = AppsRepository()

    private var banners: List<BannerItem> = emptyList()
    private var recommendedApps: List<AppItem> = emptyList()
    private var recIndex = 0

    private val bannerDelay = 30000L
    private val RECOMMENDED_COUNT = 5

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTopRow(view)
        setupRecommendedRow(view)
        setupAppsGrid(view)
        setupSidebarKey(view)

        loadAppsFromServer()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        view?.removeCallbacks(rotationRunnable)
    }

    private fun setupTopRow(root: View) {
        bannerCarousel = root.findViewById(R.id.bannerCarousel)

        banners = listOf(
            BannerItem("VLC", R.drawable.ic_vlc, "https://ya.ru"),
            BannerItem("YouTube", R.drawable.banner_youtube, "https://youtube.com")
        )

        bannerCarousel.setBanners(banners)
        bannerCarousel.onBannerClick = { openBannerLink(it.url) }
    }

    private fun setupRecommendedRow(root: View) {
        recommendedGrid = root.findViewById(R.id.recommendedGrid)

        recommendedAdapter = ArrayObjectAdapter(
            RecommendedAppPresenter { openAppDetails(it) }
        )

        recommendedGrid.apply {
            adapter = ItemBridgeAdapter(recommendedAdapter)
            setNumColumns(1)
            isFocusable = true
            descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        }
    }

    private val rotationRunnable = object : Runnable {
        override fun run() {
            if (recommendedApps.isEmpty()) return

            val items = mutableListOf<AppItem>()
            repeat(RECOMMENDED_COUNT) {
                items += recommendedApps[(recIndex + it) % recommendedApps.size]
            }

            recommendedAdapter.clear()
            recommendedAdapter.addAll(0, items)

            recIndex = (recIndex + RECOMMENDED_COUNT) % recommendedApps.size
            view?.postDelayed(this, bannerDelay)
        }
    }

    private fun loadAppsFromServer() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val apps = repository.loadApps()

                adapter.clear()
                adapter.addAll(0, apps)

                recommendedApps = apps
                    .filter { it.featured }
                    .ifEmpty { apps }
                    .shuffled()

                if (recommendedApps.isNotEmpty()) {
                    recIndex = 0
                    view?.post(rotationRunnable)
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun setupAppsGrid(root: View) {
        grid = root.findViewById(R.id.appsGrid)

        grid.apply {
            verticalSpacing = 12
            horizontalSpacing = 12
            isFocusable = true
            descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        }

        adapter = ArrayObjectAdapter(
            AppCardPresenter { openAppDetails(it) }
        )

        grid.adapter = ItemBridgeAdapter(adapter)
        updateGridColumns()
    }

    private fun setupSidebarKey(root: View) {
        root.setOnKeyListener { _, keyCode, event ->
            keyCode == KeyEvent.KEYCODE_DPAD_LEFT &&
                    event.action == KeyEvent.ACTION_DOWN &&
                    (activity as? MainActivity)?.openSidebar().let { true }
        }
    }

    private fun updateGridColumns() {
        val columns = if ((activity as? MainActivity)?.isSidebarOpen == true) 4 else 5
        grid.setNumColumns(columns)
    }

    override fun onSidebarOpened() = updateGridColumns()
    override fun onSidebarClosed() = updateGridColumns()

    private fun openBannerLink(url: String) {
        startActivity(
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    private fun openAppDetails(app: AppItem) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.main_container, AppDetailsFragment.newInstance(app))
            .addToBackStack(null)
            .commit()
    }
}
