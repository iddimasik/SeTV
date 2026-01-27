package rus.setv

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.HorizontalGridView
import androidx.leanback.widget.ItemBridgeAdapter
import androidx.leanback.widget.Presenter
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

    private lateinit var grid: VerticalGridView
    private lateinit var adapter: ArrayObjectAdapter

    private lateinit var recommendedGrid: VerticalGridView
    private lateinit var recommendedAdapter: ArrayObjectAdapter

    private lateinit var bannerCarousel: BannerCarousel
    private lateinit var filterRow: HorizontalGridView

    private val repository = AppsRepository()

    private var category = "ALL"

    private var allAppsList: List<AppItem> = emptyList()
    private var recommendedApps: List<AppItem> = emptyList()
    private var recIndex = 0

    private var currentFilter: AppFilter = AppFilter.ALL
    private val bannerDelay = 30000L
    private val RECOMMENDED_COUNT = 5

    enum class AppFilter { ALL, INSTALLED, NOT_INSTALLED, UPDATE_AVAILABLE }

    // ───────────────────────
    // LIFECYCLE
    // ───────────────────────
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        category = arguments?.getString(ARG_CATEGORY) ?: "ALL"

        setupTopRow(view)

        setupRecommendedRow(view)
        setupAppsGrid(view)
        setupSidebarKey(view)
        //setupFilterRow(view)

        loadAppsFromServer()
    }

    override fun onResume() {
        super.onResume()
        updateAllStatuses()
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

//     ───────────────────────
//     FILTER ROW
//     ───────────────────────
//    private fun setupFilterRow(root: View) {
//        filterRow = root.findViewById(R.id.filterRow)
//
//        val filters = listOf(
//            AppFilter.ALL to Pair(R.drawable.ic_apps, "Все"),
//            AppFilter.INSTALLED to Pair(R.drawable.ic_installed, "Установлено"),
//            AppFilter.NOT_INSTALLED to Pair(R.drawable.ic_uninstalled, "Не установлено"),
//            AppFilter.UPDATE_AVAILABLE to Pair(R.drawable.ic_upgrade, "Обновления")
//        )
//
//        val filterAdapter = ArrayObjectAdapter(object : Presenter() {
//            override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
//                val layout = LayoutInflater.from(parent.context)
//                    .inflate(R.layout.item_filter_button, parent, false)
//                layout.isFocusable = true
//                layout.isFocusableInTouchMode = true
//                return ViewHolder(layout)
//            }
//
//            override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
//                val pair = item as Pair<AppFilter, Pair<Int, String>>
//                val root = viewHolder.view
//                val icon = root.findViewById<ImageView>(R.id.filterIcon)
//                val text = root.findViewById<TextView>(R.id.filterText)
//
//                icon.setImageResource(pair.second.first)
//                text.text = pair.second.second
//
//                root.setOnClickListener {
//                    currentFilter = pair.first
//                    applyFilter()
//                }
//            }
//
//            override fun onUnbindViewHolder(viewHolder: ViewHolder) {}
//        })
//
//        filters.forEach { filterAdapter.add(it) }
//        filterRow.adapter = ItemBridgeAdapter(filterAdapter)
//    }
//
//    private fun applyFilter() {
//        val filtered = when (currentFilter) {
//            AppFilter.ALL -> allAppsList
//            AppFilter.INSTALLED -> allAppsList.filter { it.status == AppStatus.INSTALLED }
//            AppFilter.NOT_INSTALLED -> allAppsList.filter { it.status == AppStatus.NOT_INSTALLED }
//            AppFilter.UPDATE_AVAILABLE -> allAppsList.filter { it.status == AppStatus.UPDATE_AVAILABLE }
//        }
//
//        adapter.clear()
//        adapter.addAll(0, filtered)
//
//        recommendedApps = filtered.filter { it.featured }.ifEmpty { filtered }.shuffled()
//        recIndex = 0
//        view?.removeCallbacks(rotationRunnable)
//        view?.post(rotationRunnable)
//    }

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
    // LOAD APPS
    // ───────────────────────
    private fun loadAppsFromServer() {
        viewLifecycleOwner.lifecycleScope.launch {
            val apps = repository.loadApps()
            apps.forEach { updateStatus(it) }

            allAppsList = if (category == "ALL") apps else apps.filter { it.category.equals(category, true) }

            adapter.clear()
            adapter.addAll(0, allAppsList)

            recommendedApps = allAppsList.filter { it.featured }.ifEmpty { allAppsList }.shuffled()
            recIndex = 0
            view?.removeCallbacks(rotationRunnable)
            view?.post(rotationRunnable)
        }
    }

    // ───────────────────────
    // STATUS
    // ───────────────────────
    private fun updateAllStatuses() {
        for (i in 0 until adapter.size()) updateStatus(adapter[i] as AppItem)
        for (i in 0 until recommendedAdapter.size()) updateStatus(recommendedAdapter[i] as AppItem)
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
        val server = app.version ?: return false
        val s = server.split(".").map { it.toIntOrNull() ?: 0 }
        val i = installed.split(".").map { it.toIntOrNull() ?: 0 }
        for (idx in 0 until maxOf(s.size, i.size)) {
            val sv = s.getOrElse(idx) { 0 }
            val iv = i.getOrElse(idx) { 0 }
            if (sv > iv) return true
            if (sv < iv) return false
        }
        return false
    }

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
        val cols = if ((activity as? MainActivity)?.isSidebarOpen == true) 4 else 5
        grid.setNumColumns(cols)
    }

    override fun onSidebarOpened() = updateGridColumns()
    override fun onSidebarClosed() = updateGridColumns()

    // ───────────────────────
    // NAVIGATION
    // ───────────────────────
    private fun openBannerLink(url: String) {
        startActivity(Intent(Intent.ACTION_VIEW, url.toUri()).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }

    private fun openAppDetails(app: AppItem) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.main_container, AppDetailsFragment.newInstance(app))
            .addToBackStack(null)
            .commit()
    }
}
