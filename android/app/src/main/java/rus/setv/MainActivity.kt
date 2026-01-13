package rus.setv

import android.animation.ValueAnimator
import android.os.Bundle
import android.view.View
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var sidebar: View
    var isSidebarOpen = true

    private val SIDEBAR_OPEN_DP = 240
    private val SIDEBAR_CLOSED_DP = 60

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

    fun openSidebar(force: Boolean = false) {
        if (isSidebarOpen && !force) return
        isSidebarOpen = true

        animateSidebarDp(SIDEBAR_OPEN_DP)
        sidebar.post { sidebar.requestFocus() }
    }

    fun closeSidebar() {
        if (!isSidebarOpen) return
        isSidebarOpen = false

        animateSidebarDp(SIDEBAR_CLOSED_DP)
        findViewById<View>(R.id.main_container)?.requestFocus()
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

    private fun dpToPx(dp: Int): Int =
        (dp * resources.displayMetrics.density).toInt()
}