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

    companion object {
        private const val STATE_QUERY = "state_query"
        private const val STATE_LANG = "state_lang"
        private const val STATE_MODE = "state_mode"
    }

    // ─────────────────────────────
    // VIEW
    // ─────────────────────────────
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        queryView = view.findViewById(R.id.searchQuery)
        appsGrid = view.findViewById(R.id.appsGrid)
        keyboardGrid = view.findViewById(R.id.keyboardGrid)

        restoreState(savedInstanceState)

        setupAppsGrid()
        setupKeyboard()
        loadApps()

        queryView.text = query.toString()

        view.isFocusableInTouchMode = true
        view.requestFocus()
        view.setOnKeyListener { _, keyCode, event ->
            keyCode == KeyEvent.KEYCODE_BACK &&
                    event.action == KeyEvent.ACTION_DOWN &&
                    parentFragmentManager.popBackStack().let { true }
        }
    }

    override fun onResume() {
        super.onResume()

        keyboardGrid.post {
            keyboardGrid.findViewHolderForAdapterPosition(0)
                ?.itemView
                ?.requestFocus()
        }
    }

    // ─────────────────────────────
    // SAVE / RESTORE STATE
    // ─────────────────────────────
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString(STATE_QUERY, query.toString())
        outState.putString(STATE_LANG, currentLang.name)
        outState.putString(STATE_MODE, keyboardMode.name)
    }

    private fun restoreState(savedInstanceState: Bundle?) {
        savedInstanceState ?: return

        savedInstanceState.getString(STATE_QUERY)?.let {
            query.clear()
            query.append(it)
        }

        savedInstanceState.getString(STATE_LANG)?.let {
            currentLang = KeyboardLang.valueOf(it)
        }

        savedInstanceState.getString(STATE_MODE)?.let {
            keyboardMode = KeyboardMode.valueOf(it)
        }
    }

    // ─────────────────────────────
    // APPS GRID
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

            repository.loadApps()
                .onSuccess { apps ->
                    allApps = apps
                    updateResults()
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
                showError("Ошибка загрузки приложений")
        }
    }

    private fun showError(message: String) {
        android.widget.Toast
            .makeText(requireContext(), message, android.widget.Toast.LENGTH_LONG)
            .show()
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
                        "␣", "⌫", "🌐", "123", "ABC" -> 2
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
