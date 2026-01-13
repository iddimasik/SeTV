package rus.setv

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.fragment.app.Fragment

class SidebarFragment : Fragment(R.layout.fragment_sidebar) {

    private lateinit var appsItem: LinearLayout
    private lateinit var gamesItem: LinearLayout
    private lateinit var settingsItem: LinearLayout

    private lateinit var appsText: TextView
    private lateinit var gamesText: TextView
    private lateinit var settingsText: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        appsItem = view.findViewById(R.id.menu_apps)
        gamesItem = view.findViewById(R.id.menu_games)
        settingsItem = view.findViewById(R.id.menu_settings)

        appsText = view.findViewById(R.id.text_apps)
        gamesText = view.findViewById(R.id.text_games)
        settingsText = view.findViewById(R.id.text_settings)

        setupItem(appsItem, appsText) { openContent(CatalogFragment()) }
        setupItem(gamesItem, gamesText) { /* TODO GamesFragment */ }
        setupItem(settingsItem, settingsText) { /* TODO SettingsFragment */ }

        // ⚡ Показываем текст сразу, если sidebar открыт по умолчанию
        (activity as? MainActivity)?.let { main ->
            if (main.isSidebarOpen) onSidebarOpened()
        }

        // Фокус по умолчанию
        appsItem.requestFocus()
    }

    private fun setupItem(item: LinearLayout, text: TextView, onClick: () -> Unit) {
        // Клик по элементу
        item.setOnClickListener {
            onClick()
            closeSidebar()
        }

        // Разворачивание при фокусе
        item.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                (activity as? MainActivity)?.openSidebar()
            }
        }

        // Сворачивание при DPAD_RIGHT
        item.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && event.action == KeyEvent.ACTION_DOWN) {
                (activity as? MainActivity)?.closeSidebar()
                false
            } else {
                false
            }
        }
    }

    // ────────────── PUBLIC ──────────────

    fun onSidebarOpened() {
        appsText.visibility = View.VISIBLE
        gamesText.visibility = View.VISIBLE
        settingsText.visibility = View.VISIBLE
    }

    fun onSidebarClosed() {
        appsText.visibility = View.GONE
        gamesText.visibility = View.GONE
        settingsText.visibility = View.GONE
    }

    // ────────────── HELPERS ──────────────

    private fun openContent(fragment: Fragment) {
        requireActivity().supportFragmentManager.beginTransaction()
            .replace(R.id.main_container, fragment)
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
