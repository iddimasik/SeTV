package rus.setv

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.net.toUri
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.ItemBridgeAdapter
import androidx.leanback.widget.VerticalGridView
import kotlinx.coroutines.launch
import rus.setv.data.repository.AppsRepository
import rus.setv.model.AppItem
import rus.setv.model.AppStatus
import rus.setv.model.BannerItem
import rus.setv.ui.AppsGridAdapter
import rus.setv.ui.BannerCarousel
import rus.setv.ui.RecommendedAppPresenter

class CatalogFragment : Fragment(R.layout.fragment_catalog),
    MainActivity.SidebarListener {

    companion object {
        private const val ARG_CATEGORY = "category"

        fun newInstance(category: String) = CatalogFragment().apply {
            arguments = Bundle().apply {
                putString(ARG_CATEGORY, category)
            }
        }
    }

    // UI
    private lateinit var grid: RecyclerView
    private lateinit var gridAdapter: AppsGridAdapter
    private lateinit var recommendedGrid: VerticalGridView
    private lateinit var recommendedAdapter: ArrayObjectAdapter
    private lateinit var bannerCarousel: BannerCarousel
    private lateinit var topRow: View
    private lateinit var filtersScrollView: View

    private lateinit var filterAll: View
    private lateinit var filterInstalled: View
    private lateinit var filterNotInstalled: View
    private lateinit var filterUpdates: View

    private lateinit var bannerDotsContainer: LinearLayout

    // DATA
    private val repository = AppsRepository()
    private var category = "ALL"
    private var allAppsList: List<AppItem> = emptyList()
    private var recommendedApps: List<AppItem> = emptyList()
    private var selectedAppBeforeDetails: AppItem? = null

    // FILTERS
    enum class StatusFilter { ALL, INSTALLED, NOT_INSTALLED, UPDATE_AVAILABLE }
    private var currentStatusFilter = StatusFilter.ALL

    // RECOMMENDED ROTATION
    private var recIndex = 0
    private val bannerDelay = 30000L

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

        if (allAppsList.isEmpty()) {
            loadAppsFromServer()
        }
    }

    override fun onResume() {
        super.onResume()

        if (selectedAppBeforeDetails != null) {
            // Returning from app details - restore focus
            val appToFocus = selectedAppBeforeDetails
            selectedAppBeforeDetails = null

            // Update statuses and reapply filter to refresh grid
            updateAllStatuses()
            applyStatusFilter()

            // Find the app position in grid
            grid.post {
                // Search through ViewHolders to find the app
                for (i in 0 until gridAdapter.itemCount) {
                    val viewHolder = grid.findViewHolderForAdapterPosition(i)
                    if (viewHolder != null) {
                        // Already visible, check if match
                        // Will find via scrolling below
                    }
                }

                // Just scroll and request focus
                // gridAdapter doesn't expose items, so we use position from allAppsList
                val filteredList = when (currentStatusFilter) {
                    StatusFilter.ALL -> allAppsList
                    StatusFilter.INSTALLED -> allAppsList.filter { it.status == AppStatus.INSTALLED }
                    StatusFilter.NOT_INSTALLED -> allAppsList.filter { it.status == AppStatus.NOT_INSTALLED }
                    StatusFilter.UPDATE_AVAILABLE -> allAppsList.filter { it.status == AppStatus.UPDATE_AVAILABLE }
                }

                val position = filteredList.indexOfFirst {
                    it.packageName == appToFocus?.packageName
                }

                if (position >= 0 && position < gridAdapter.itemCount) {
                    // Hide/show topRow based on position
                    if (position >= 3) {
                        hideTopRow()
                    } else {
                        showTopRow()
                    }

                    grid.scrollToPosition(position)
                    grid.postDelayed({
                        grid.findViewHolderForAdapterPosition(position)?.itemView?.requestFocus()
                    }, 150)
                } else {
                    // App not found, focus first item
                    showTopRow()
                    grid.postDelayed({
                        grid.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus()
                    }, 150)
                }
            }

            // Restart recommended rotation
            restartRecommendedRotation()
        } else {
            // Normal resume
            updateAllStatuses()
            applyStatusFilter()

            bannerCarousel.post {
                bannerCarousel.requestFocus()
            }
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
        bannerDotsContainer = root.findViewById(R.id.bannerDotsContainer)

        val banners = listOf(
            BannerItem("VLC", R.drawable.ic_vlc, "https://www.videolan.org"),
            BannerItem("YouTube", R.drawable.banner_youtube, "https://youtube.com")
        )

        bannerCarousel.setBanners(banners)

        createBannerDots(banners.size)

        bannerCarousel.onBannerChanged = { index ->
            updateBannerDots(index)
        }

        bannerCarousel.onBannerClick = { openBannerLink(it.url) }
        bannerCarousel.onLeftKey = { openSidebarAndFocus() }
    }

    private fun createBannerDots(count: Int) {
        bannerDotsContainer.removeAllViews()

        repeat(count) {
            val dot = View(requireContext())

            val size = (6 * resources.displayMetrics.density).toInt()
            val margin = (6 * resources.displayMetrics.density).toInt()

            val params = LinearLayout.LayoutParams(size, size)
            params.setMargins(margin, 0, margin, 0)

            dot.layoutParams = params
            dot.setBackgroundResource(R.drawable.dot_selector)

            bannerDotsContainer.addView(dot)
        }

        updateBannerDots(0)
    }

    private fun updateBannerDots(activeIndex: Int) {
        for (i in 0 until bannerDotsContainer.childCount) {
            val dot = bannerDotsContainer.getChildAt(i)
            dot.isSelected = i == activeIndex
        }
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
        android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_LONG)
            .show()
    }

    // ───────────────────────
    // STATUS FILTERS
    // ───────────────────────
    private fun setupStatusFilters(root: View) {
        filterAll = root.findViewById(R.id.filterStatusAll)
        filterInstalled = root.findViewById(R.id.filterStatusInstalled)
        filterNotInstalled = root.findViewById(R.id.filterStatusNotInstalled)
        filterUpdates = root.findViewById(R.id.filterStatusUpdates)

        filterAll.findViewById<TextView>(R.id.filterText).text = "Все"
        filterInstalled.findViewById<TextView>(R.id.filterText).text = "Установлено"
        filterNotInstalled.findViewById<TextView>(R.id.filterText).text = "Не установлено"
        filterUpdates.findViewById<TextView>(R.id.filterText).text = "Обновления"

        filterAll.findViewById<ImageView>(R.id.filterIcon).setImageResource(R.drawable.ic_apps)
        filterInstalled.findViewById<ImageView>(R.id.filterIcon)
            .setImageResource(R.drawable.ic_installed)
        filterNotInstalled.findViewById<ImageView>(R.id.filterIcon)
            .setImageResource(R.drawable.ic_uninstalled)
        filterUpdates.findViewById<ImageView>(R.id.filterIcon)
            .setImageResource(R.drawable.ic_upgrade)

        filterAll.setOnClickListener {
            currentStatusFilter = StatusFilter.ALL
            applyStatusFilter()
            updateFilterSelection()
        }
        filterInstalled.setOnClickListener {
            currentStatusFilter = StatusFilter.INSTALLED
            applyStatusFilter()
            updateFilterSelection()
        }
        filterNotInstalled.setOnClickListener {
            currentStatusFilter = StatusFilter.NOT_INSTALLED
            applyStatusFilter()
            updateFilterSelection()
        }
        filterUpdates.setOnClickListener {
            currentStatusFilter = StatusFilter.UPDATE_AVAILABLE
            applyStatusFilter()
            updateFilterSelection()
        }

        // UP from filterAll, filterInstalled, filterNotInstalled → banner
        val filterUpToBanner = View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP && event.action == KeyEvent.ACTION_DOWN) {
                bannerCarousel.requestFocus()
                true
            } else false
        }
        filterAll.setOnKeyListener(filterUpToBanner)
        filterInstalled.setOnKeyListener(filterUpToBanner)
        filterNotInstalled.setOnKeyListener(filterUpToBanner)

        // UP from filterUpdates → last recommended item
        filterUpdates.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_DPAD_UP && event.action == KeyEvent.ACTION_DOWN) {
                val lastPos = recommendedAdapter.size() - 1
                if (lastPos >= 0) {
                    recommendedGrid.findViewHolderForAdapterPosition(lastPos)
                        ?.itemView?.requestFocus()
                } else {
                    bannerCarousel.requestFocus()
                }
                true
            } else false
        }

        // Set initial selection
        updateFilterSelection()
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
                textView.setTextColor(0xFF09E490.toInt())
                iconView.setColorFilter(0xFF09E490.toInt())
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

        gridAdapter.setApps(filtered)

        // Debug: log first 10 apps in adapter
        android.util.Log.d("CatalogFragment", "=== ADAPTER CONTENT ===")
        for (i in 0 until minOf(10, filtered.size)) {
            android.util.Log.d("CatalogFragment", "Position $i: ${filtered[i].name}")
        }
        android.util.Log.d("CatalogFragment", "Total: ${filtered.size} apps")
        android.util.Log.d("CatalogFragment", "======================")

        grid.post {
            if (filtered.isNotEmpty()) {
                grid.scrollToPosition(0)
                grid.findViewHolderForAdapterPosition(0)?.itemView?.requestFocus()
            }
        }
    }

    // ───────────────────────
    // STATUS
    // ───────────────────────
    private fun updateAllStatuses() {
        allAppsList.forEach { updateStatus(it) }
    }

    private fun updateStatus(app: AppItem) {
        val installed = getInstalledVersionName(app.packageName)
        app.status = when {
            installed == null -> AppStatus.NOT_INSTALLED
            isUpdateAvailable(app.version, installed) -> AppStatus.UPDATE_AVAILABLE
            else -> AppStatus.INSTALLED
        }
    }

    private fun getInstalledVersionName(packageName: String): String? {
        return try {
            requireContext().packageManager.getPackageInfo(packageName, 0).versionName
        } catch (e: PackageManager.NameNotFoundException) {
            null
        }
    }

    private fun isUpdateAvailable(server: String?, installed: String?): Boolean {
        if (server.isNullOrBlank() || installed.isNullOrBlank()) return false
        val s = normalizeVersion(server)
        val i = normalizeVersion(installed)
        for (idx in 0 until maxOf(s.size, i.size)) {
            if (s.getOrElse(idx) { 0 } > i.getOrElse(idx) { 0 }) return true
            if (s.getOrElse(idx) { 0 } < i.getOrElse(idx) { 0 }) return false
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

        gridAdapter = AppsGridAdapter { openAppDetails(it) }

        gridAdapter.onFirstRowNavigateUp = { position ->
            val col = position % 3
            android.util.Log.d("CatalogFragment", "UP from pos=$position, col=$col")
            when (col) {
                0 -> filterAll.requestFocus()
                1 -> filterNotInstalled.requestFocus()
                2 -> filterUpdates.requestFocus()
            }
        }

        gridAdapter.onNavigateLeft = { position ->
            android.util.Log.d("CatalogFragment", "LEFT from pos=$position")
            openSidebarAndFocus()
        }

        grid.apply {
            layoutManager = GridLayoutManager(requireContext(), 3)
            adapter = gridAdapter
            isFocusable = true
            descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS

            // Add spacing between items
            addItemDecoration(object : RecyclerView.ItemDecoration() {
                override fun getItemOffsets(
                    outRect: android.graphics.Rect,
                    view: View,
                    parent: RecyclerView,
                    state: RecyclerView.State
                ) {
                    outRect.set(6, 6, 6, 6) // 12dp spacing = 6dp on each side
                }
            })

            // Hide/show topRow based on scroll position
            addOnScrollListener(object : RecyclerView.OnScrollListener() {
                override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                    super.onScrolled(recyclerView, dx, dy)

                    val layoutManager = recyclerView.layoutManager as? GridLayoutManager
                    if (layoutManager != null) {
                        val firstVisiblePosition = layoutManager.findFirstVisibleItemPosition()
                        val lastVisiblePosition = layoutManager.findLastVisibleItemPosition()

                        // Check if any element from first row (0,1,2) is visible
                        val firstRowVisible = (0..2).any { pos ->
                            pos in firstVisiblePosition..lastVisiblePosition
                        }

                        // Hide topRow when first row is not visible (scrolled to second row or below)
                        if (!firstRowVisible) {
                            hideTopRow()
                        } else {
                            showTopRow()
                        }
                    }
                }
            })
        }
    }

    // updateGridColumns not needed - GridLayoutManager always has 3 columns

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

    private fun openSidebarAndFocus() {
        val mainActivity = activity as? MainActivity
        mainActivity?.openSidebar()

        view?.postDelayed({
            val sidebar = requireActivity().findViewById<View>(R.id.sidebar_container)
            val searchItem = sidebar?.findViewById<View>(R.id.menu_search)
            searchItem?.requestFocus()
        }, 50)
    }

    // ───────────────────────
    // SIDEBAR
    // ───────────────────────
    override fun onSidebarOpened() {
        // No need to update grid columns - always 3
        restartRecommendedRotation()
    }

    override fun onSidebarClosed() {
        // No need to update grid columns - always 3
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
        selectedAppBeforeDetails = app

        // Close sidebar BEFORE fragment transaction to prevent focus jump
        (activity as? MainActivity)?.closeSidebar()

        parentFragmentManager.beginTransaction()
            .replace(R.id.main_container, AppDetailsFragment.newInstance(app))
            .addToBackStack(null)
            .commit()
    }
}
