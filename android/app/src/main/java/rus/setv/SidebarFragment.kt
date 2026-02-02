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

class SidebarFragment : Fragment(R.layout.fragment_sidebar) {

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
        setupItem(settingsItem, settingsText) { /* TODO SettingsFragment */ }

        if ((activity as? MainActivity)?.isSidebarOpen == true) {
            onSidebarOpened()
        }

        searchItem.requestFocus()
    }

    // ───────────────────────
    // ITEM SETUP
    // ───────────────────────
    private fun setupItem(item: LinearLayout, text: TextView, onClick: () -> Unit) {

        item.setOnClickListener {
            onClick()
            closeSidebar()
        }

        item.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) openSidebar()
        }

        item.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT &&
                event.action == KeyEvent.ACTION_DOWN
            ) {
                closeSidebar()
                false
            } else {
                false
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
    // UPDATE APP
    // ───────────────────────
    private fun openUpdateApp() {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                val apps = repository.loadApps()
                val updateApp = apps.firstOrNull {
                    it.packageName == UPDATE_APP_PACKAGE
                }
                if (updateApp != null) {
                    openAppDetails(updateApp)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    // ───────────────────────
    // SIDEBAR STATE
    // ───────────────────────
    fun onSidebarOpened() {
        searchText.visibility = View.VISIBLE
        allAppsText.visibility = View.VISIBLE
        moviesText.visibility = View.VISIBLE
        programsText.visibility = View.VISIBLE
        otherText.visibility = View.VISIBLE
        updateText.visibility = View.VISIBLE
        settingsText.visibility = View.VISIBLE
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
        (activity as? MainActivity)?.closeSidebar()
    }
}
