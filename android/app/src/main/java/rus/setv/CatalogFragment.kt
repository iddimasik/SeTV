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
import rus.setv.ui.RecommendedAppView

class CatalogFragment : Fragment(R.layout.fragment_catalog) {

    // ───────────────────────
    // UI
    // ───────────────────────
    private lateinit var grid: VerticalGridView
    private lateinit var adapter: ArrayObjectAdapter
    private lateinit var bannerCarousel: BannerCarousel

    private lateinit var recommendedView1: RecommendedAppView
    private lateinit var recommendedView2: RecommendedAppView

    // ───────────────────────
    // DATA
    // ───────────────────────
    private val repository = AppsRepository()
    private var lastSelectedPosition = 0

    private var banners: List<BannerItem> = emptyList()
    private var recommendedApps: List<AppItem> = emptyList()
    private var recIndex = 0

    private val bannerDelay = 5000L

    // ───────────────────────
    // LIFECYCLE
    // ───────────────────────
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupTopRow(view)
        setupAppsGrid(view)
        setupSidebarKey(view)

        loadAppsFromServer()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        view?.removeCallbacks(rotationRunnable)
    }

    // ───────────────────────
    // TOP ROW (БАННЕР + 2 РЕКОМЕНДАЦИИ)
    // ───────────────────────
    private fun setupTopRow(root: View) {
        bannerCarousel = root.findViewById(R.id.bannerCarousel)
        recommendedView1 = root.findViewById(R.id.recommendedApp1)
        recommendedView2 = root.findViewById(R.id.recommendedApp2)

        banners = listOf(
            BannerItem("VLC", R.drawable.ic_vlc, "https://ya.ru"),
            BannerItem("YouTube", R.drawable.banner_youtube, "https://youtube.com")
        )
        bannerCarousel.setBanners(banners)

        bannerCarousel.onBannerClick = { banner ->
            openBannerLink(banner.url)
        }

        // клики по рекомендациям
        recommendedView1.onAppClick = { openAppDetails(it) }
        recommendedView2.onAppClick = { openAppDetails(it) }
    }

    // ───────────────────────
    // ROTATION (2 КАРТОЧКИ)
    // ───────────────────────
    private val rotationRunnable = object : Runnable {
        override fun run() {
            if (recommendedApps.size < 2) return

            val first = recommendedApps[recIndex % recommendedApps.size]
            val second = recommendedApps[(recIndex + 1) % recommendedApps.size]

            recommendedView1.bind(first)
            recommendedView2.bind(second)

            recIndex = (recIndex + 2) % recommendedApps.size

            view?.postDelayed(this, bannerDelay)
        }
    }

    private fun startRotation() {
        view?.removeCallbacks(rotationRunnable)
        view?.post(rotationRunnable)
    }

    // ───────────────────────
    // GRID
    // ───────────────────────
    private fun setupAppsGrid(root: View) {
        grid = root.findViewById(R.id.appsGrid)

        grid.apply {
            verticalSpacing = 12
            horizontalSpacing = 12
            isFocusable = true
            isFocusableInTouchMode = true
            descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        }

        adapter = ArrayObjectAdapter(
            AppCardPresenter { app ->
                lastSelectedPosition = adapter.indexOf(app)
                openAppDetails(app)
            }
        )

        grid.adapter = ItemBridgeAdapter(adapter)

        // Изначально устанавливаем количество колонок
        updateGridColumns()
    }

    // ───────────────────────
    // LOAD DATA
    // ───────────────────────
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

                when (recommendedApps.size) {
                    0 -> {
                        recommendedView1.visibility = View.GONE
                        recommendedView2.visibility = View.GONE
                    }

                    1 -> {
                        recommendedView1.visibility = View.VISIBLE
                        recommendedView2.visibility = View.VISIBLE
                        recommendedView1.bind(recommendedApps[0])
                        recommendedView2.bind(recommendedApps[0])
                    }

                    else -> {
                        recommendedView1.visibility = View.VISIBLE
                        recommendedView2.visibility = View.VISIBLE
                        recIndex = 0
                        startRotation()
                    }
                }

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
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

    private fun openBannerLink(url: String) {
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun updateGridColumns() {
        val mainActivity = activity as? MainActivity ?: return
        val columns = if (mainActivity.isSidebarOpen) 4 else 5
        grid.setNumColumns(columns)
    }

    // ───────────────────────
    // DETAILS
    // ───────────────────────
    private fun openAppDetails(app: AppItem) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.main_container, AppDetailsFragment.newInstance(app))
            .addToBackStack(null)
            .commit()
    }
}
