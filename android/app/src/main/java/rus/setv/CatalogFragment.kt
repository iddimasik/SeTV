package rus.setv

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

class CatalogFragment : Fragment(R.layout.fragment_catalog) {

    private lateinit var grid: VerticalGridView
    private lateinit var adapter: ArrayObjectAdapter

    private val repository = AppsRepository()
    private var lastSelectedPosition: Int = 0

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupBanner(view)
        setupAppsGrid(view)
        setupSidebarKey(view)

        loadAppsFromServer()
    }

    override fun onResume() {
        super.onResume()
        restoreFocus()
    }

    // ───────────────────────
    // БАННЕР
    // ───────────────────────
    private fun setupBanner(root: View) {
        val banner = root.findViewById<BannerCarousel>(R.id.bannerCarousel)

        banner.setBanners(
            listOf(
                BannerItem("VLC", R.drawable.ic_vlc),
                BannerItem("YouTube", R.drawable.banner_youtube)
            )
        )
    }

    // ───────────────────────
    // СЕТКА ПРИЛОЖЕНИЙ
    // ───────────────────────
    private fun setupAppsGrid(root: View) {
        grid = root.findViewById(R.id.appsGrid)

        grid.apply {
            setNumColumns(4)
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
    }

    // ───────────────────────
    // ЗАГРУЗКА С СЕРВЕРА
    // ───────────────────────
    private fun loadAppsFromServer() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val apps: List<AppItem> = repository.loadApps()

                adapter.clear()
                adapter.addAll(0, apps)

                restoreFocus()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ───────────────────────
    // ВОССТАНОВЛЕНИЕ ФОКУСА
    // ───────────────────────
    private fun restoreFocus() {
        if (!::grid.isInitialized) return
        if (!::adapter.isInitialized) return
        if (adapter.size() == 0) return

        grid.post {
            val position = lastSelectedPosition.coerceIn(0, adapter.size() - 1)
            grid.setSelectedPosition(position)

            grid.findViewHolderForAdapterPosition(position)
                ?.itemView
                ?.requestFocus()
        }
    }

    // ───────────────────────
    // LEFT → SIDEBAR
    // ───────────────────────
    private fun setupSidebarKey(root: View) {
        root.setOnKeyListener { _, keyCode, event ->
            if (
                keyCode == KeyEvent.KEYCODE_DPAD_LEFT &&
                event.action == KeyEvent.ACTION_DOWN
            ) {
                (activity as? MainActivity)?.openSidebar()
                true
            } else {
                false
            }
        }
    }

    // ───────────────────────
    // DETAILS
    // ───────────────────────
    private fun openAppDetails(app: AppItem) {
        parentFragmentManager.beginTransaction()
            .replace(
                R.id.main_container,
                AppDetailsFragment.newInstance(app)
            )
            .addToBackStack(null)
            .commit()
    }
}