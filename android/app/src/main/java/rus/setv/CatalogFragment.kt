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
    private lateinit var topRow: View
    private lateinit var filtersScrollView: View

    private lateinit var filterAll: View
    private lateinit var filterInstalled: View
    private lateinit var filterNotInstalled: View
    private lateinit var filterUpdates: View

    // DATA
    private val repository = AppsRepository()
    private var category = "ALL"
    private var allAppsList: List<AppItem> = emptyList()
    private var recommendedApps: List<AppItem> = emptyList()
    private var currentGridColumns = 4

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

        topRow = view.findViewById(R.id.topRow)
        filtersScrollView = view.findViewById(R.id.filtersScrollView)

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

        val presenter = RecommendedAppPresenter { openAppDetails(it) }
        presenter.onLastItemNavigateDown = {
            filterUpdates.requestFocus()
        }
        presenter.isLastItemProvider = {
            recommendedGrid.selectedPosition == recommendedAdapter.size() - 1
        }

        recommendedAdapter = ArrayObjectAdapter(presenter)

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

            repository.loadApps()
                .onSuccess { apps ->

                    apps.forEach { updateStatus(it) }

                    allAppsList =
                        if (category == "ALL") apps
                        else apps.filter { it.category.equals(category, true) }

                    recommendedApps =
                        allAppsList.filter { it.featured }
                            .ifEmpty { allAppsList }
                            .shuffled()

                    applyStatusFilter()
                    restartRecommendedRotation()
                }
                .onFailure { e ->
                    handleLoadError(e)
                }
        }
    }

    private fun handleLoadError(e: Throwable) {
        when (e) {
            is java.net.SocketTimeoutException ->
                showError("Сервер не отвечает")

            is java.io.IOException ->
                showError("Нет подключения к интернету")

            else ->
                showError("Ошибка загрузки приложений")
        }
    }

    private fun showError(message: String) {
        android.widget.Toast
            .makeText(requireContext(), message, android.widget.Toast.LENGTH_LONG)
            .show()
    }

    // ───────────────────────
    // STATUS FILTER UI
    // ───────────────────────
    private fun setupStatusFilters(root: View) {
        filterAll = root.findViewById(R.id.filterStatusAll)
        filterInstalled = root.findViewById(R.id.filterStatusInstalled)
        filterNotInstalled = root.findViewById(R.id.filterStatusNotInstalled)
        filterUpdates = root.findViewById(R.id.filterStatusUpdates)

        bindStatusFilter(filterAll, "Все", R.drawable.ic_apps, StatusFilter.ALL)
        bindStatusFilter(filterInstalled, "Установлено", R.drawable.ic_installed, StatusFilter.INSTALLED)
        bindStatusFilter(filterNotInstalled, "Не установлено", R.drawable.ic_uninstalled, StatusFilter.NOT_INSTALLED)
        bindStatusFilter(filterUpdates, "Обновления", R.drawable.ic_upgrade, StatusFilter.UPDATE_AVAILABLE)

        updateFilterSelection()
    }

    private fun bindStatusFilter(
        btn: View,
        title: String,
        iconRes: Int,
        filter: StatusFilter
    ) {
        btn.findViewById<TextView>(R.id.filterText).text = title
        btn.findViewById<ImageView>(R.id.filterIcon).setImageResource(iconRes)

        btn.setOnClickListener {
            currentStatusFilter = filter
            applyStatusFilter()
            updateFilterSelection()
        }

        btn.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                btn.setBackgroundResource(R.drawable.filter_focus_background)
            } else {
                btn.setBackgroundColor(0x00000000)
            }
        }
    }

    private fun updateFilterSelection() {
        val allFilters = listOf(filterAll, filterInstalled, filterNotInstalled, filterUpdates)
        val selectedFilter = when (currentStatusFilter) {
            StatusFilter.ALL -> filterAll
            StatusFilter.INSTALLED -> filterInstalled
            StatusFilter.NOT_INSTALLED -> filterNotInstalled
            StatusFilter.UPDATE_AVAILABLE -> filterUpdates
        }

        allFilters.forEach { filter ->
            val textView = filter.findViewById<TextView>(R.id.filterText)
            val iconView = filter.findViewById<ImageView>(R.id.filterIcon)

            if (filter == selectedFilter) {
                textView.setTextColor(0xFF2196F3.toInt())
                iconView.setColorFilter(0xFF2196F3.toInt())
            } else {
                textView.setTextColor(0xFFFFFFFF.toInt())
                iconView.setColorFilter(0xFFFFFFFF.toInt())
            }
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

        val presenter = AppCardPresenter { openAppDetails(it) }
        presenter.onFirstRowNavigateUp = {
            filterAll.requestFocus()
        }
        presenter.isFirstRowProvider = {
            grid.selectedPosition < currentGridColumns
        }

        adapter = ArrayObjectAdapter(presenter)

        grid.apply {
            adapter = ItemBridgeAdapter(this@CatalogFragment.adapter)
            verticalSpacing = 12
            horizontalSpacing = 12
            isFocusable = true
            descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS

            setOnChildViewHolderSelectedListener(object : androidx.leanback.widget.OnChildViewHolderSelectedListener() {
                override fun onChildViewHolderSelected(
                    parent: androidx.recyclerview.widget.RecyclerView,
                    child: androidx.recyclerview.widget.RecyclerView.ViewHolder?,
                    position: Int,
                    subposition: Int
                ) {
                    if (position >= currentGridColumns) {
                        hideTopRow()
                    } else {
                        showTopRow()
                    }
                }
            })
        }

        updateGridColumns()
    }

    private fun hideTopRow() {
        if (topRow.visibility == View.VISIBLE) {
            topRow.animate()
                .alpha(0f)
                .scaleY(0f)
                .setDuration(150)
                .withEndAction {
                    topRow.visibility = View.GONE
                }
                .start()
        }
    }

    private fun showTopRow() {
        if (topRow.visibility != View.VISIBLE) {
            topRow.visibility = View.VISIBLE
            topRow.scaleY = 0f
            topRow.alpha = 0f

            topRow.animate()
                .alpha(1f)
                .scaleY(1f)
                .setDuration(150)
                .start()
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

    private fun updateGridColumns() {
        currentGridColumns = if ((activity as? MainActivity)?.isSidebarOpen == true) 3 else 4
        grid.setNumColumns(currentGridColumns)
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
