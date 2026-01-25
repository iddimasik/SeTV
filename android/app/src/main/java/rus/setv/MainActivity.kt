package rus.setv

import android.animation.ValueAnimator
import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var sidebar: View
    var isSidebarOpen = true
        private set

    private val SIDEBAR_OPEN_DP = 240
    private val SIDEBAR_CLOSED_DP = 60

    // ───────────────────────
    // WARNING PREFS
    // ───────────────────────
    private val PREFS_NAME = "app_prefs"
    private val KEY_WARNING_ACCEPTED = "warning_accepted"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        sidebar = findViewById(R.id.sidebar_container)

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

    // ───────────────────────
    // WARNING DIALOG
    // ───────────────────────

    private fun showWarningIfNeeded() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val accepted = prefs.getBoolean(KEY_WARNING_ACCEPTED, false)

        if (accepted) return

        val dialogView = LayoutInflater.from(this)
            .inflate(R.layout.dialog_warning, null)

        val dialog = AlertDialog.Builder(
            this,
            android.R.style.Theme_DeviceDefault_Dialog_NoActionBar
        )
            .setView(dialogView)
            .setCancelable(false)
            .create()

        dialogView.findViewById<View>(R.id.btn_accept).setOnClickListener {
            prefs.edit()
                .putBoolean(KEY_WARNING_ACCEPTED, true)
                .apply()
            dialog.dismiss()
        }

        dialogView.findViewById<View>(R.id.btn_cancel).apply {
            setOnClickListener {
                dialog.dismiss()
                finishAffinity()
            }
            requestFocus()
        }

        dialog.show()
    }

    // ───────────────────────
    // SIDEBAR CONTROL
    // ───────────────────────

    fun openSidebar(force: Boolean = false) {
        if (isSidebarOpen && !force) return
        isSidebarOpen = true

        animateSidebarDp(SIDEBAR_OPEN_DP)
        sidebar.post { sidebar.requestFocus() }

        notifySidebarOpened()
    }

    fun closeSidebar() {
        if (!isSidebarOpen) return
        isSidebarOpen = false

        animateSidebarDp(SIDEBAR_CLOSED_DP)
        findViewById<View>(R.id.main_container)?.requestFocus()

        notifySidebarClosed()
    }

    // ───────────────────────
    // NOTIFY LISTENERS
    // ───────────────────────

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

    // ───────────────────────
    // ANIMATION
    // ───────────────────────

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

    // ───────────────────────
    // INTERFACE
    // ───────────────────────

    interface SidebarListener {
        fun onSidebarOpened()
        fun onSidebarClosed()
    }

    // ───────────────────────
    // UTILS
    // ───────────────────────

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()
}
