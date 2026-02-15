package rus.setv

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import rus.setv.data.repository.AppsRepository
import rus.setv.model.AppItem

class SidebarFragment : Fragment(R.layout.fragment_sidebar), MainActivity.SidebarListener {

    private lateinit var searchItem: LinearLayout
    private lateinit var allAppsItem: LinearLayout
    private lateinit var moviesItem: LinearLayout
    private lateinit var programsItem: LinearLayout
    private lateinit var otherItem: LinearLayout
    private lateinit var updateItem: LinearLayout
    private lateinit var settingsItem: LinearLayout

    private lateinit var searchText: TextView
    private lateinit var allAppsText: TextView
    private lateinit var moviesText: TextView
    private lateinit var programsText: TextView
    private lateinit var otherText: TextView
    private lateinit var updateText: TextView
    private lateinit var settingsText: TextView

    private val repository = AppsRepository()
    private val UPDATE_APP_PACKAGE = "rus.setv"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ───── ITEMS
        searchItem = view.findViewById(R.id.menu_search)
        allAppsItem = view.findViewById(R.id.menu_apps)
        moviesItem = view.findViewById(R.id.menu_movies)
        programsItem = view.findViewById(R.id.menu_programs)
        otherItem = view.findViewById(R.id.menu_other)
        updateItem = view.findViewById(R.id.menu_update)
        settingsItem = view.findViewById(R.id.menu_settings)

        // ───── TEXTS
        searchText = view.findViewById(R.id.text_search)
        allAppsText = view.findViewById(R.id.text_apps)
        moviesText = view.findViewById(R.id.text_movies)
        programsText = view.findViewById(R.id.text_programs)
        otherText = view.findViewById(R.id.text_other)
        updateText = view.findViewById(R.id.text_update)
        settingsText = view.findViewById(R.id.text_settings)

        // ───── LABELS
        searchText.text = "Поиск"
        allAppsText.text = "Все приложения"
        moviesText.text = "Фильмы и ТВ"
        programsText.text = "Программы"
        otherText.text = "Прочее"
        updateText.text = "Обновить приложение"
        settingsText.text = "О приложении"

        // ───── SETUP
        setupItem(searchItem, searchText) { openSearch() }
        setupItem(allAppsItem, allAppsText) { openContent(CatalogFragment.newInstance("ALL")) }
        setupItem(moviesItem, moviesText) { openContent(CatalogFragment.newInstance("Фильмы и ТВ")) }
        setupItem(programsItem, programsText) { openContent(CatalogFragment.newInstance("Программы")) }
        setupItem(otherItem, otherText) { openContent(CatalogFragment.newInstance("Прочее")) }
        setupItem(updateItem, updateText) { openUpdateApp() }
        setupItem(settingsItem, settingsText, closeSidebarOnClick = false) { openSettings() }  // Keep sidebar open!

        // Sidebar starts closed, make items not focusable to prevent focus jumps
        searchItem.isFocusable = false
        allAppsItem.isFocusable = false
        moviesItem.isFocusable = false
        programsItem.isFocusable = false
        otherItem.isFocusable = false
        updateItem.isFocusable = false
        settingsItem.isFocusable = false
    }

    // ───────────────────────
    // ITEM SETUP
    // ───────────────────────
    private fun setupItem(item: LinearLayout, text: TextView, closeSidebarOnClick: Boolean = true, onClick: () -> Unit) {

        item.setOnKeyListener { _, keyCode, event ->
            when {
                (keyCode == KeyEvent.KEYCODE_DPAD_CENTER || keyCode == KeyEvent.KEYCODE_ENTER) &&
                        event.action == KeyEvent.ACTION_DOWN -> {
                    if (closeSidebarOnClick) {
                        closeSidebar()
                    }
                    onClick()
                    true
                }
                keyCode == KeyEvent.KEYCODE_DPAD_RIGHT &&
                        event.action == KeyEvent.ACTION_DOWN -> {
                    if (closeSidebarOnClick) {
                        closeSidebar()
                    }
                    transferFocusToBanner()
                    true
                }
                else -> false
            }
        }
    }

    // ───────────────────────
    // SEARCH
    // ───────────────────────
    private fun openSearch() {
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.main_container, SearchFragment())
            .addToBackStack(null)
            .commit()

        closeSidebar()
    }

    // ───────────────────────
    // SETTINGS
    // ───────────────────────
    private fun openSettings() {
        // NOTE: Don't close sidebar - settings has no content to focus on
        // Sidebar stays open with focus on settings item
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.main_container, SettingsFragment())
            .addToBackStack(null)
            .commit()
    }

    // ───────────────────────
    // UPDATE APP
    // ───────────────────────
    private fun openUpdateApp() {
        viewLifecycleOwner.lifecycleScope.launch {

            repository.loadApps()
                .onSuccess { apps ->
                    val updateApp = apps.firstOrNull {
                        it.packageName == UPDATE_APP_PACKAGE
                    }

                    if (updateApp != null) {
                        openAppDetails(updateApp)
                    } else {
                        showError("Обновление не найдено")
                    }
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
                showError("Ошибка загрузки данных")
        }
    }

    private fun showError(message: String) {
        android.widget.Toast
            .makeText(requireContext(), message, android.widget.Toast.LENGTH_LONG)
            .show()
    }

    // ───────────────────────
    // SIDEBAR STATE
    // ───────────────────────
    override fun onSidebarOpened() {
        searchText.visibility = View.VISIBLE
        allAppsText.visibility = View.VISIBLE
        moviesText.visibility = View.VISIBLE
        programsText.visibility = View.VISIBLE
        otherText.visibility = View.VISIBLE
        updateText.visibility = View.VISIBLE
        settingsText.visibility = View.VISIBLE

        // Make items focusable when sidebar opens
        searchItem.isFocusable = true
        allAppsItem.isFocusable = true
        moviesItem.isFocusable = true
        programsItem.isFocusable = true
        otherItem.isFocusable = true
        updateItem.isFocusable = true
        settingsItem.isFocusable = true
    }

    override fun onSidebarClosed() {
        searchText.visibility = View.GONE
        allAppsText.visibility = View.GONE
        moviesText.visibility = View.GONE
        programsText.visibility = View.GONE
        otherText.visibility = View.GONE
        updateText.visibility = View.GONE
        settingsText.visibility = View.GONE

        // Make items NOT focusable when sidebar closes - prevents focus jumps
        searchItem.isFocusable = false
        allAppsItem.isFocusable = false
        moviesItem.isFocusable = false
        programsItem.isFocusable = false
        otherItem.isFocusable = false
        updateItem.isFocusable = false
        settingsItem.isFocusable = false
    }

    // ───────────────────────
    // NAVIGATION
    // ───────────────────────
    private fun openContent(fragment: Fragment) {
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.main_container, fragment)
            .addToBackStack(null)
            .commit()
    }

    private fun openAppDetails(app: AppItem) {
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.main_container, AppDetailsFragment.newInstance(app))
            .addToBackStack(null)
            .commit()
    }

    private fun openSidebar() {
        (activity as? MainActivity)?.openSidebar()
    }

    private fun closeSidebar() {
        android.util.Log.d("SidebarFragment", "closeSidebar() called, isSidebarOpen=${(activity as? MainActivity)?.isSidebarOpen}")
        (activity as? MainActivity)?.closeSidebar()
    }

    private fun transferFocusToBanner() {
        val mainContainer = requireActivity().findViewById<View>(R.id.main_container)

        // Check if current fragment is AppDetailsFragment
        val currentFragment = requireActivity().supportFragmentManager
            .findFragmentById(R.id.main_container)

        if (currentFragment is AppDetailsFragment) {
            // Focus back button in app details
            mainContainer?.post {
                val backButton = mainContainer.findViewById<View>(R.id.backButton)
                backButton?.requestFocus()
            }
            return
        }

        // Otherwise check if top row is visible
        val topRow = mainContainer?.findViewById<View>(R.id.topRow)
        val banner = topRow?.findViewById<View>(R.id.bannerCarousel)
        val grid = mainContainer?.findViewById<View>(R.id.appsGrid)

        // If top row is hidden, focus grid instead
        if (topRow?.visibility == View.GONE) {
            grid?.post {
                grid.requestFocus()
            }
        } else {
            banner?.post {
                banner.requestFocus()
            }
        }
    }
}
