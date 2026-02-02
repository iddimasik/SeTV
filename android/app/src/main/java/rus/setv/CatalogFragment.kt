package rus.setv

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.ItemBridgeAdapter
import androidx.leanback.widget.VerticalGridView
import kotlinx.coroutines.launch
import rus.setv.data.repository.AppsRepository
import rus.setv.model.AppItem
import rus.setv.model.AppStatus
import rus.setv.model.BannerItem
import rus.setv.ui.AppCardPresenter
import rus.setv.ui.BannerCarousel
import rus.setv.ui.RecommendedAppPresenter

class CatalogFragment : Fragment(R.layout.fragment_catalog),
    MainActivity.SidebarListener {

    companion object {
        private const val ARG_CATEGORY = "category"

        fun newInstance(category: String) =
            CatalogFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_CATEGORY, category)
                }
            }
    }

    // UI
    private lateinit var grid: VerticalGridView
    private lateinit var adapter: ArrayObjectAdapter
    private lateinit var recommendedGrid: VerticalGridView
    private lateinit var recommendedAdapter: ArrayObjectAdapter
    private lateinit var bannerCarousel: BannerCarousel

    // DATA
    private val repository = AppsRepository()
    private var category = "ALL"
    private var allAppsList: List<AppItem> = emptyList()
    private var recommendedApps: List<AppItem> = emptyList()

    // STATUS FILTER
    private enum class StatusFilter {
        ALL, INSTALLED, NOT_INSTALLED, UPDATE_AVAILABLE
    }

    private var currentStatusFilter = StatusFilter.ALL

    private var recIndex = 0
    private val bannerDelay = 30_000L

    // ───────────────────────
    // LIFECYCLE
    // ───────────────────────
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        category = arguments?.getString(ARG_CATEGORY) ?: "ALL"

        setupTopRow(view)
        setupRecommendedRow(view)
        setupStatusFilters(view)
        setupAppsGrid(view)
        setupSidebarKey(view)

        loadAppsFromServer()
    }

    override fun onResume() {
        super.onResume()
        updateAllStatuses()
        applyStatusFilter()

        bannerCarousel.post {
            bannerCarousel.requestFocus()
        }
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
        bannerCarousel.setBanners(
            listOf(
                BannerItem("VLC", R.drawable.ic_vlc, "https://www.videolan.org"),
                BannerItem("YouTube", R.drawable.banner_youtube, "https://youtube.com")
            )
        )
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

            val count = 4
            val items = mutableListOf<AppItem>()

            repeat(count) {
                items += recommendedApps[(recIndex + it) % recommendedApps.size]
            }

            recommendedAdapter.clear()
            recommendedAdapter.addAll(0, items)

            recommendedGrid.post {
                if (recommendedAdapter.size() > 0) {
                    recommendedGrid.setSelectedPosition(0)
                }
            }

            recIndex = (recIndex + count) % recommendedApps.size
            view?.postDelayed(this, bannerDelay)
        }
    }

    private fun restartRecommendedRotation() {
        recIndex = 0
        view?.removeCallbacks(rotationRunnable)
        view?.post(rotationRunnable)
    }

    // ───────────────────────
    // LOAD APPS
    // ───────────────────────
    private fun loadAppsFromServer() {
        viewLifecycleOwner.lifecycleScope.launch {
            val apps = repository.loadApps()
            apps.forEach { updateStatus(it) }

            allAppsList =
                if (category == "ALL") apps
                else apps.filter { it.category.equals(category, true) }

            recommendedApps =
                allAppsList.filter { it.featured }.ifEmpty { allAppsList }.shuffled()

            applyStatusFilter()
            restartRecommendedRotation()
        }
    }

    // ───────────────────────
    // STATUS FILTER UI
    // ───────────────────────
    private fun setupStatusFilters(root: View) {
        bindStatusFilter(root, R.id.filterStatusAll, "Все",R.drawable.ic_apps,StatusFilter.ALL)
        bindStatusFilter(root, R.id.filterStatusInstalled, "Установлено",R.drawable.ic_installed, StatusFilter.INSTALLED)
        bindStatusFilter(root, R.id.filterStatusNotInstalled, "Не установлено",R.drawable.ic_uninstalled, StatusFilter.NOT_INSTALLED)
        bindStatusFilter(root, R.id.filterStatusUpdates, "Обновления",R.drawable.ic_upgrade, StatusFilter.UPDATE_AVAILABLE)
    }

    private fun bindStatusFilter(
        root: View,
        id: Int,
        title: String,
        iconRes: Int,
        filter: StatusFilter
    ) {
        val btn = root.findViewById<View>(id)

        btn.findViewById<TextView>(R.id.filterText).text = title
        btn.findViewById<ImageView>(R.id.filterIcon)
            .setImageResource(iconRes)

        btn.setOnClickListener {
            currentStatusFilter = filter
            applyStatusFilter()
        }
    }


    private fun applyStatusFilter() {
        val filtered = when (currentStatusFilter) {
            StatusFilter.ALL -> allAppsList
            StatusFilter.INSTALLED ->
                allAppsList.filter { it.status == AppStatus.INSTALLED }
            StatusFilter.NOT_INSTALLED ->
                allAppsList.filter { it.status == AppStatus.NOT_INSTALLED }
            StatusFilter.UPDATE_AVAILABLE ->
                allAppsList.filter { it.status == AppStatus.UPDATE_AVAILABLE }
        }

        adapter.clear()
        adapter.addAll(0, filtered)

        grid.post {
            if (adapter.size() > 0) {
                grid.setSelectedPosition(0)
            } else {
                grid.clearFocus()
            }
        }
    }

    // ───────────────────────
    // STATUS
    // ───────────────────────
    private fun updateAllStatuses() {
        allAppsList.forEach { updateStatus(it) }
        adapter.notifyArrayItemRangeChanged(0, adapter.size())
        recommendedAdapter.notifyArrayItemRangeChanged(0, recommendedAdapter.size())
    }

    private fun updateStatus(app: AppItem) {
        val installedVersion = getInstalledVersion(app.packageName)
        app.status = when {
            installedVersion == null -> AppStatus.NOT_INSTALLED
            isUpdateAvailable(app, installedVersion) -> AppStatus.UPDATE_AVAILABLE
            else -> AppStatus.INSTALLED
        }
    }

    private fun getInstalledVersion(pkg: String): String? =
        try {
            requireContext().packageManager.getPackageInfo(pkg, 0).versionName
        } catch (_: PackageManager.NameNotFoundException) {
            null
        }

    private fun isUpdateAvailable(app: AppItem, installed: String): Boolean {
        val serverVersion = normalizeVersion(app.version ?: return false)
        val installedVersion = normalizeVersion(installed)

        val max = maxOf(serverVersion.size, installedVersion.size)
        for (i in 0 until max) {
            val s = serverVersion.getOrElse(i) { 0 }
            val iV = installedVersion.getOrElse(i) { 0 }
            if (s > iV) return true
            if (s < iV) return false
        }
        return false
    }

    private fun normalizeVersion(version: String): List<Int> =
        version.replace(Regex("[^0-9.]"), "")
            .split(".")
            .filter { it.isNotBlank() }
            .take(5)
            .map { it.toIntOrNull() ?: 0 }

    // ───────────────────────
    // GRID
    // ───────────────────────
    private fun setupAppsGrid(root: View) {
        grid = root.findViewById(R.id.appsGrid)
        adapter = ArrayObjectAdapter(AppCardPresenter { openAppDetails(it) })

        grid.apply {
            adapter = ItemBridgeAdapter(this@CatalogFragment.adapter)
            verticalSpacing = 12
            horizontalSpacing = 12
            isFocusable = true
            descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        }

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
        val cols = if ((activity as? MainActivity)?.isSidebarOpen == true) 3 else 4
        grid.setNumColumns(cols)
    }

    override fun onSidebarOpened() {
        updateGridColumns()
        restartRecommendedRotation()
    }

    override fun onSidebarClosed() {
        updateGridColumns()
        restartRecommendedRotation()
    }

    // ───────────────────────
    // NAVIGATION
    // ───────────────────────
    private fun openBannerLink(url: String) {
        startActivity(
            Intent(Intent.ACTION_VIEW, url.toUri())
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
