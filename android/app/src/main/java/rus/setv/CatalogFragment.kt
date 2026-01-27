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

    companion object {
        private const val ARG_CATEGORY = "category"

        fun newInstance(category: String): CatalogFragment {
            return CatalogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CATEGORY, category)
                }
            }
        }
    }

    private lateinit var grid: VerticalGridView
    private lateinit var adapter: ArrayObjectAdapter
    private lateinit var bannerCarousel: BannerCarousel

    private lateinit var recommendedGrid: VerticalGridView
    private lateinit var recommendedAdapter: ArrayObjectAdapter

    private val repository = AppsRepository()

    private var category: String = "ALL"

    private var banners: List<BannerItem> = emptyList()
    private var recommendedApps: List<AppItem> = emptyList()
    private var recIndex = 0

    private val bannerDelay = 30_000L
    private val RECOMMENDED_COUNT = 5

    // ───────────────────────
    // LIFECYCLE
    // ───────────────────────
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        category = arguments?.getString(ARG_CATEGORY, "ALL") ?: "ALL"

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

    // ───────────────────────
    // TOP BANNERS
    // ───────────────────────
    private fun setupTopRow(root: View) {
        bannerCarousel = root.findViewById(R.id.bannerCarousel)

        banners = listOf(
            BannerItem("VLC", R.drawable.ic_vlc, "https://www.videolan.org"),
            BannerItem("YouTube", R.drawable.banner_youtube, "https://youtube.com")
        )

        bannerCarousel.setBanners(banners)
        bannerCarousel.onBannerClick = { openBannerLink(it.url) }
    }

    // ───────────────────────
    // RECOMMENDED
    // ───────────────────────
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

    // ───────────────────────
    // LOAD + FILTER APPS
    // ───────────────────────
    private fun loadAppsFromServer() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val allApps = repository.loadApps()

                val filteredApps = when (category) {
                    "ALL" -> allApps
                    else -> allApps.filter {
                        it.category.equals(category, ignoreCase = true)
                    }
                }

                adapter.clear()
                adapter.addAll(0, filteredApps)

                recommendedApps = filteredApps
                    .filter { it.featured }
                    .ifEmpty { filteredApps }
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

    // ───────────────────────
    // APPS GRID
    // ───────────────────────
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

    // ───────────────────────
    // SIDEBAR
    // ───────────────────────
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

    // ───────────────────────
    // NAVIGATION
    // ───────────────────────
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
