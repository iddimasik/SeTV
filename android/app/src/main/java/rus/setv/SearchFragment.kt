package rus.setv

import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.ItemBridgeAdapter
import androidx.leanback.widget.VerticalGridView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import rus.setv.data.repository.AppsRepository
import rus.setv.model.AppItem
import rus.setv.ui.AppCardPresenter

class SearchFragment : Fragment(R.layout.fragment_search),
    MainActivity.SidebarListener {

    // ───── QUERY
    private lateinit var queryView: TextView
    private val query = StringBuilder()

    // ───── RESULTS
    private lateinit var appsGrid: VerticalGridView
    private lateinit var appsAdapter: ArrayObjectAdapter

    // ───── KEYBOARD
    private lateinit var keyboardGrid: RecyclerView
    private lateinit var keyboardAdapter: KeyboardAdapter
    private lateinit var keyboardLayoutManager: GridLayoutManager

    private val repository = AppsRepository()
    private var allApps: List<AppItem> = emptyList()

    // ───── KEYBOARD STATE
    private var currentLang = KeyboardLang.RU
    private var keyboardMode = KeyboardMode.LETTERS

    enum class KeyboardLang { RU, EN }
    enum class KeyboardMode { LETTERS, NUMBERS }

    // ─────────────────────────────
    // VIEW
    // ─────────────────────────────
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        queryView = view.findViewById(R.id.searchQuery)
        appsGrid = view.findViewById(R.id.appsGrid)
        keyboardGrid = view.findViewById(R.id.keyboardGrid)

        setupAppsGrid()
        setupKeyboard()
        loadApps()

        view.isFocusableInTouchMode = true
        view.requestFocus()
        view.setOnKeyListener { _, keyCode, event ->
            keyCode == KeyEvent.KEYCODE_BACK &&
                    event.action == KeyEvent.ACTION_DOWN &&
                    parentFragmentManager.popBackStack().let { true }
        }
    }

    // ─────────────────────────────
    // APPS GRID (КАК В CATALOG)
    // ─────────────────────────────
    private fun setupAppsGrid() {
        appsAdapter = ArrayObjectAdapter(
            AppCardPresenter { openAppDetails(it) }
        )

        appsGrid.apply {
            adapter = ItemBridgeAdapter(appsAdapter)
            verticalSpacing = 12
            horizontalSpacing = 12
            isFocusable = true
            descendantFocusability = ViewGroup.FOCUS_AFTER_DESCENDANTS
        }

        updateGridColumns()
    }

    private fun updateGridColumns() {
        val cols =
            if ((activity as? MainActivity)?.isSidebarOpen == true) 3 else 4
        appsGrid.setNumColumns(cols)
    }

    override fun onSidebarOpened() = updateGridColumns()
    override fun onSidebarClosed() = updateGridColumns()

    // ─────────────────────────────
    // DATA
    // ─────────────────────────────
    private fun loadApps() {
        viewLifecycleOwner.lifecycleScope.launch {
            allApps = repository.loadApps()
            updateResults()
        }
    }

    private fun updateResults() {
        val q = query.toString().trim().lowercase()

        val filtered =
            if (q.isEmpty()) emptyList()
            else allApps.filter { it.name.lowercase().contains(q) }

        appsAdapter.clear()
        appsAdapter.addAll(0, filtered)
    }

    // ─────────────────────────────
    // KEYBOARD
    // ─────────────────────────────
    private fun setupKeyboard() {
        keyboardLayoutManager = GridLayoutManager(requireContext(), 8)

        keyboardAdapter = KeyboardAdapter(buildKeyboard()) {
            onKeyPressed(it)
        }

        keyboardLayoutManager.spanSizeLookup =
            object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    val key = keyboardAdapter.getKey(position)
                    return when (key) {
                        "␣" -> 2
                        "⌫", "🌐", "123", "ABC" -> 2
                        "Z" -> 7
                        else -> 1
                    }
                }
            }

        keyboardGrid.layoutManager = keyboardLayoutManager
        keyboardGrid.adapter = keyboardAdapter
        keyboardGrid.isFocusable = true
    }

    private fun onKeyPressed(key: String) {
        when (key) {
            "⌫" -> if (query.isNotEmpty()) query.deleteAt(query.length - 1)
            "␣" -> query.append(" ")
            "🌐" -> toggleLanguage()

            "123" -> {
                keyboardMode = KeyboardMode.NUMBERS
                keyboardAdapter.setKeys(buildKeyboard())
                return
            }

            "ABC" -> {
                keyboardMode = KeyboardMode.LETTERS
                keyboardAdapter.setKeys(buildKeyboard())
                return
            }

            else -> query.append(key)
        }

        queryView.text = query.toString()
        updateResults()
    }

    private fun toggleLanguage() {
        currentLang =
            if (currentLang == KeyboardLang.RU) KeyboardLang.EN
            else KeyboardLang.RU

        keyboardMode = KeyboardMode.LETTERS
        keyboardAdapter.setKeys(buildKeyboard())
    }

    // ─────────────────────────────
    // NAVIGATION
    // ─────────────────────────────
    private fun openAppDetails(app: AppItem) {
        parentFragmentManager.beginTransaction()
            .replace(R.id.main_container, AppDetailsFragment.newInstance(app))
            .addToBackStack(null)
            .commit()
    }

    // ─────────────────────────────
    // KEYBOARD LAYOUTS
    // ─────────────────────────────
    private fun buildKeyboard(): List<String> =
        when (keyboardMode) {

            KeyboardMode.LETTERS ->
                when (currentLang) {
                    KeyboardLang.RU -> listOf(
                        "А","Б","В","Г","Д","Е","Ж","З",
                        "И","Й","К","Л","М","Н","О","П",
                        "Р","С","Т","У","Ф","Х","Ц","Ч",
                        "Ш","Щ","Ъ","Ы","Ь","Э","Ю","Я",
                        "123","🌐","␣","⌫"
                    )

                    KeyboardLang.EN -> listOf(
                        "A","B","C","D","E","F","G","H",
                        "I","J","K","L","M","N","O","P",
                        "Q","R","S","T","U","V","W","X",
                        "Y","Z",
                        "123","🌐","␣","⌫"
                    )
                }

            KeyboardMode.NUMBERS -> listOf(
                "1","2","3","4","5","6","7","8",
                "9","0","-","_","+",
                ".",",",":",
                "ABC","␣","⌫"
            )
        }
}
