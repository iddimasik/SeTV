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

    private lateinit var appsItem: LinearLayout
    private lateinit var gamesItem: LinearLayout
    private lateinit var updateItem: LinearLayout
    private lateinit var settingsItem: LinearLayout

    private lateinit var appsText: TextView
    private lateinit var gamesText: TextView
    private lateinit var updateText: TextView
    private lateinit var settingsText: TextView

    private val repository = AppsRepository()

    private val UPDATE_APP_PACKAGE = "rus.setv"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        appsItem = view.findViewById(R.id.menu_apps)
        gamesItem = view.findViewById(R.id.menu_games)
        updateItem = view.findViewById(R.id.menu_update)
        settingsItem = view.findViewById(R.id.menu_settings)

        appsText = view.findViewById(R.id.text_apps)
        gamesText = view.findViewById(R.id.text_games)
        updateText = view.findViewById(R.id.text_update)
        settingsText = view.findViewById(R.id.text_settings)

        setupItem(appsItem, appsText) {
            openContent(CatalogFragment())
        }

        setupItem(gamesItem, gamesText) {
            /* TODO GamesFragment */
        }

        setupItem(updateItem, updateText) {
            openUpdateApp()
        }

        setupItem(settingsItem, settingsText) {
            /* TODO SettingsFragment */
        }

        (activity as? MainActivity)?.let { main ->
            if (main.isSidebarOpen) onSidebarOpened()
        }

        appsItem.requestFocus()
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
            if (hasFocus) {
                openSidebar()
            }
        }

        item.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && event.action == KeyEvent.ACTION_DOWN) {
                closeSidebar()
                false
            } else {
                false
            }
        }
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
        appsText.visibility = View.VISIBLE
        gamesText.visibility = View.VISIBLE
        updateText.visibility = View.VISIBLE
        settingsText.visibility = View.VISIBLE
    }

    fun onSidebarClosed() {
        appsText.visibility = View.GONE
        gamesText.visibility = View.GONE
        updateText.visibility = View.GONE
        settingsText.visibility = View.GONE
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
