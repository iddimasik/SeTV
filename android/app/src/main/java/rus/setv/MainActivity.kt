package rus.setv

import android.animation.ValueAnimator
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var sidebar: View
    private lateinit var dimOverlay: View
    private lateinit var mainContainer: ViewGroup
    var isSidebarOpen = false  // Closed by default
        private set
    private var blockSidebarReopening = false

    private val SIDEBAR_OPEN_DP = 200
    private val SIDEBAR_CLOSED_DP = 60

    private val PREFS_NAME = "app_prefs"
    private val KEY_WARNING_ACCEPTED = "warning_accepted"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sidebar = findViewById(R.id.sidebar_container)
        dimOverlay = findViewById(R.id.dim_overlay)
        mainContainer = findViewById(R.id.main_container)

        // Set sidebar to closed size initially
        val params = sidebar.layoutParams
        params.width = dpToPx(SIDEBAR_CLOSED_DP)
        sidebar.layoutParams = params

        // Dim overlay initially hidden
        dimOverlay.visibility = View.GONE
        dimOverlay.alpha = 0f

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.sidebar_container, SidebarFragment())
                .replace(R.id.main_container, CatalogFragment())
                .commit()
        }

        showWarningIfNeeded()

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (isSidebarOpen) {
                    closeSidebar()
                } else {
                    isEnabled = false
                    onBackPressedDispatcher.onBackPressed()
                }
            }
        })
    }

    private fun showWarningIfNeeded() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val accepted = prefs.getBoolean(KEY_WARNING_ACCEPTED, false)

        if (accepted) return

        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.dialog_warning, null)

        val btnAccept = dialogView.findViewById<View>(R.id.btn_accept)
        val btnCancel = dialogView.findViewById<View>(R.id.btn_cancel)

        val dialog = AlertDialog.Builder(
            this,
            android.R.style.Theme_DeviceDefault_Dialog_NoActionBar
        )
            .setView(dialogView)
            .setCancelable(false)
            .create()

        btnAccept.setOnClickListener {
            prefs.edit()
                .putBoolean(KEY_WARNING_ACCEPTED, true)
                .apply()
            dialog.dismiss()
        }

        btnCancel.setOnClickListener {
            dialog.dismiss()
            finishAffinity()
        }

        dialog.setOnShowListener {
            btnCancel.requestFocus()
        }

        dialog.show()
    }


    fun openSidebar(force: Boolean = false) {
        android.util.Log.d("MainActivity", "openSidebar() called, isSidebarOpen=$isSidebarOpen, force=$force, blocked=$blockSidebarReopening")
        if (isSidebarOpen && !force) {
            android.util.Log.d("MainActivity", "Already open, returning")
            return
        }
        if (!force && blockSidebarReopening) {
            android.util.Log.d("MainActivity", "Temporarily blocked, not reopening")
            return
        }
        isSidebarOpen = true

        animateSidebarDp(SIDEBAR_OPEN_DP)

        // Show and fade in dim overlay
        dimOverlay.visibility = View.VISIBLE
        dimOverlay.animate()
            .alpha(1f)
            .setDuration(220)
            .start()

        // Block focus on main content
        mainContainer.descendantFocusability = ViewGroup.FOCUS_BLOCK_DESCENDANTS

        // Show text labels when sidebar opens
        notifySidebarOpened()

        android.util.Log.d("MainActivity", "openSidebar() finished")
    }

    fun closeSidebar() {
        android.util.Log.d("MainActivity", "closeSidebar() called, isSidebarOpen=$isSidebarOpen")
        if (!isSidebarOpen) return
        isSidebarOpen = false
        blockSidebarReopening = true
        android.util.Log.d("MainActivity", "Animating sidebar to CLOSED")
        animateSidebarDp(SIDEBAR_CLOSED_DP)

        // Fade out and hide dim overlay
        dimOverlay.animate()
            .alpha(0f)
            .setDuration(220)
            .withEndAction {
                dimOverlay.visibility = View.GONE
            }
            .start()

        // Restore focus on main content
        mainContainer.descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS

        // DON'T request focus on main_container - it causes focus to jump to sidebar
        notifySidebarClosed()
        android.util.Log.d("MainActivity", "closeSidebar() finished")

        sidebar.postDelayed({
            blockSidebarReopening = false
            android.util.Log.d("MainActivity", "Block removed, sidebar can reopen now")
        }, 500)
    }

    fun hideDimOverlay() {
        dimOverlay.animate()
            .alpha(0f)
            .setDuration(220)
            .withEndAction {
                dimOverlay.visibility = View.GONE
            }
            .start()
    }

    fun showDimOverlay() {
        if (isSidebarOpen) {
            dimOverlay.visibility = View.VISIBLE
            dimOverlay.animate()
                .alpha(1f)
                .setDuration(220)
                .start()
        }
    }

    private fun notifySidebarOpened() {
        supportFragmentManager.fragments.forEach { fragment ->
            if (fragment is SidebarListener) {
                fragment.onSidebarOpened()
            }
        }
    }

    private fun notifySidebarClosed() {
        supportFragmentManager.fragments.forEach { fragment ->
            if (fragment is SidebarListener) {
                fragment.onSidebarClosed()
            }
        }
    }

    private fun animateSidebarDp(targetDp: Int) {
        val targetPx = dpToPx(targetDp)

        val params = sidebar.layoutParams
        val startWidth = params.width.takeIf { it > 0 } ?: targetPx

        ValueAnimator.ofInt(startWidth, targetPx).apply {
            duration = 220
            addUpdateListener { animator ->
                params.width = animator.animatedValue as Int
                sidebar.layoutParams = params
            }
            start()
        }
    }

    interface SidebarListener {
        fun onSidebarOpened()
        fun onSidebarClosed()
    }

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()
}
