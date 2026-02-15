package rus.setv

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.fragment.app.Fragment

class SettingsFragment : Fragment(R.layout.fragment_settings),
    MainActivity.SidebarListener {

    private lateinit var appNameText: TextView
    private lateinit var appVersionText: TextView

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        appNameText = view.findViewById(R.id.text_app_name)
        appVersionText = view.findViewById(R.id.text_app_version)

        loadAppInfo()

        // Make sure root can receive key events
        view.isFocusable = true
        view.isFocusableInTouchMode = true

        // Intercept BACK press BEFORE MainActivity gets it
        requireActivity().onBackPressedDispatcher.addCallback(viewLifecycleOwner, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                android.util.Log.d("SettingsFragment", "BACK pressed, popping back stack")
                parentFragmentManager.popBackStack()
            }
        })

        // Also handle via key listener as fallback
        view.setOnKeyListener { _, keyCode, event ->
            if (keyCode == android.view.KeyEvent.KEYCODE_BACK &&
                event.action == android.view.KeyEvent.ACTION_DOWN) {
                android.util.Log.d("SettingsFragment", "BACK key via listener, popping back stack")
                parentFragmentManager.popBackStack()
                true
            } else {
                false
            }
        }
    }

    override fun onResume() {
        super.onResume()

        android.util.Log.d("SettingsFragment", "onResume called")

        val mainActivity = activity as? MainActivity
        android.util.Log.d("SettingsFragment", "Current sidebar state: ${mainActivity?.isSidebarOpen}")

        // Sidebar should already be open (not closed by SidebarFragment)
        // Just ensure it's open in case something went wrong
        if (mainActivity?.isSidebarOpen != true) {
            android.util.Log.d("SettingsFragment", "WARNING: Sidebar was closed, reopening with force")
            mainActivity?.openSidebar(force = true)
        }

        // Focus on settings item in sidebar
        view?.postDelayed({
            android.util.Log.d("SettingsFragment", "Attempting to focus settings item")
            val sidebar = requireActivity().findViewById<View>(R.id.sidebar_container)
            val settingsItem = sidebar?.findViewById<View>(R.id.menu_settings)

            if (settingsItem != null) {
                android.util.Log.d("SettingsFragment", "Settings item found, requesting focus")
                settingsItem.requestFocus()

                // Verify focus
                settingsItem.postDelayed({
                    android.util.Log.d("SettingsFragment", "Settings item hasFocus: ${settingsItem.hasFocus()}")
                }, 100)
            } else {
                android.util.Log.e("SettingsFragment", "Settings item NOT found!")
            }
        }, 100)  // Short delay since sidebar should already be open
    }

    // Prevent sidebar column changes
    override fun onSidebarOpened() {
        // Do nothing - no grid to update
    }

    override fun onSidebarClosed() {
        // Do nothing - no grid to update
    }

    private fun loadAppInfo() {
        try {
            val context = requireContext()
            val packageManager = context.packageManager
            val packageName = "rus.setv"

            val packageInfo = packageManager.getPackageInfo(
                packageName,
                PackageManager.GET_ACTIVITIES
            )

            val appName = "A-Store"

            val versionName = packageInfo.versionName ?: "—"

            appNameText.text = appName
            appVersionText.text = "Версия $versionName"

        } catch (e: Exception) {
            appNameText.text = "Приложение"
            appVersionText.text = "Версия —"
            e.printStackTrace()
        }
    }
}
