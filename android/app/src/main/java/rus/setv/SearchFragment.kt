package rus.setv

import android.content.Intent
import android.os.Bundle
import android.speech.RecognizerIntent
import android.view.KeyEvent
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.leanback.widget.ArrayObjectAdapter
import androidx.leanback.widget.ItemBridgeAdapter
import androidx.leanback.widget.VerticalGridView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import rus.setv.adapter.KeyboardAdapter
import rus.setv.data.repository.AppsRepository
import rus.setv.model.AppItem
import rus.setv.ui.AppCardPresenter

class SearchFragment : Fragment(R.layout.fragment_search),
    MainActivity.SidebarListener {

    // ───── QUERY
    private lateinit var queryView: TextView
    private val query = StringBuilder()
    private var cursorPosition = 0  // Track cursor position
    private var cursorVisible = true
    private val cursorBlinkRunnable = object : Runnable {
        override fun run() {
            cursorVisible = !cursorVisible
            android.util.Log.d("SearchFragment", "Cursor blink: $cursorVisible")
            updateQueryDisplay()
            view?.postDelayed(this, 500)  // Blink every 500ms
        }
    }

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

    // ───── VOICE SEARCH
    private val voiceSearchLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            val matches = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)
            if (!matches.isNullOrEmpty()) {
                val voiceQuery = matches[0]
                android.util.Log.d("SearchFragment", "Voice search result: $voiceQuery")

                // Clear current query and insert voice result
                query.clear()
                query.append(voiceQuery)
                cursorPosition = query.length
                cursorVisible = true

                updateQueryDisplay()
                updateResults()
            }
        }
    }

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

        cursorPosition = query.length
        updateQueryDisplay()

        view.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_BACK && event.action == KeyEvent.ACTION_DOWN) {
                parentFragmentManager.popBackStack()
                true
            } else {
                false
            }
        }
    }

    override fun onResume() {
        super.onResume()

        keyboardGrid.post {
            keyboardGrid.findViewHolderForAdapterPosition(0)
                ?.itemView
                ?.requestFocus()
        }

        // Start cursor blinking when fragment is visible
        view?.postDelayed(cursorBlinkRunnable, 500)
    }

    override fun onPause() {
        super.onPause()
        // Stop cursor blinking when fragment is not visible
        view?.removeCallbacks(cursorBlinkRunnable)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Stop cursor blinking
        view?.removeCallbacks(cursorBlinkRunnable)
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
        val presenter = AppCardPresenter { openAppDetails(it) }
        presenter.onFirstRowNavigateUp = {
            val targetPosition = when (keyboardMode) {
                KeyboardMode.LETTERS -> when (currentLang) {
                    KeyboardLang.RU -> 32  // "123" button for Russian
                    KeyboardLang.EN -> 26  // "123" button for English
                }
                KeyboardMode.NUMBERS -> 17  // "ABC" button for numbers
            }
            keyboardGrid.findViewHolderForAdapterPosition(targetPosition)?.itemView?.requestFocus()
        }
        presenter.isFirstRowProvider = {
            val cols = if ((activity as? MainActivity)?.isSidebarOpen == true) 3 else 4
            appsGrid.selectedPosition < cols
        }

        // Add LEFT key handler to open sidebar from first column
        presenter.onNavigateLeft = {
            openSidebarAndFocus()
        }
        presenter.isFirstColumnProvider = {
            val cols = if ((activity as? MainActivity)?.isSidebarOpen == true) 3 else 4
            appsGrid.selectedPosition % cols == 0
        }

        appsAdapter = ArrayObjectAdapter(presenter)

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
            else {
                // Create transliterated versions of query
                val queryRuToEn = transliterateRuToEn(q)
                val queryEnToRu = transliterateEnToRu(q)

                allApps.filter { app ->
                    val appNameLower = app.name.lowercase()

                    // Search in original name
                    appNameLower.contains(q) ||
                            // Search in transliterated RU->EN name
                            appNameLower.contains(queryRuToEn) ||
                            // Search in transliterated EN->RU name
                            appNameLower.contains(queryEnToRu) ||
                            // Search query in transliterated app name (RU->EN)
                            transliterateRuToEn(appNameLower).contains(q) ||
                            // Search query in transliterated app name (EN->RU)
                            transliterateEnToRu(appNameLower).contains(q)
                }
            }

        appsAdapter.clear()
        appsAdapter.addAll(0, filtered)
    }

    // ─────────────────────────────
    // TRANSLITERATION
    // ─────────────────────────────
    private fun transliterateRuToEn(text: String): String {
        val map = mapOf(
            'а' to "a", 'б' to "b", 'в' to "v", 'г' to "g", 'д' to "d",
            'е' to "e", 'ё' to "yo", 'ж' to "zh", 'з' to "z", 'и' to "i",
            'й' to "y", 'к' to "k", 'л' to "l", 'м' to "m", 'н' to "n",
            'о' to "o", 'п' to "p", 'р' to "r", 'с' to "s", 'т' to "t",
            'у' to "u", 'ф' to "f", 'х' to "h", 'ц' to "ts", 'ч' to "ch",
            'ш' to "sh", 'щ' to "sch", 'ъ' to "", 'ы' to "y", 'ь' to "",
            'э' to "e", 'ю' to "yu", 'я' to "ya"
        )

        return text.map { char ->
            map[char] ?: char.toString()
        }.joinToString("")
    }

    private fun transliterateEnToRu(text: String): String {
        var result = text

        // Multi-character replacements first
        val multiChar = mapOf(
            "shch" to "щ", "sch" to "щ",
            "yo" to "ё", "zh" to "ж", "ts" to "ц",
            "ch" to "ч", "sh" to "ш", "yu" to "ю", "ya" to "я"
        )

        multiChar.forEach { (en, ru) ->
            result = result.replace(en, ru)
        }

        // Single character replacements
        val singleChar = mapOf(
            'a' to 'а', 'b' to 'б', 'v' to 'в', 'g' to 'г', 'd' to 'д',
            'e' to 'е', 'z' to 'з', 'i' to 'и', 'y' to 'й', 'k' to 'к',
            'l' to 'л', 'm' to 'м', 'n' to 'н', 'o' to 'о', 'p' to 'п',
            'r' to 'р', 's' to 'с', 't' to 'т', 'u' to 'у', 'f' to 'ф',
            'h' to 'х'
        )

        result = result.map { char ->
            singleChar[char] ?: char
        }.joinToString("")

        return result
    }

    // ─────────────────────────────
    // KEYBOARD
    // ─────────────────────────────
    private fun setupKeyboard() {
        keyboardLayoutManager = GridLayoutManager(requireContext(), 8)

        keyboardAdapter = KeyboardAdapter(buildKeyboard()) {
            onKeyPressed(it)
        }

        // Add LEFT key handler to open sidebar from first column
        keyboardAdapter.onLeftKeyFromFirstColumn = {
            openSidebarAndFocus()
        }

        // Add results provider to block DOWN when no results
        keyboardAdapter.hasResultsProvider = {
            appsAdapter.size() > 0
        }

        keyboardLayoutManager.spanSizeLookup =
            object : GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    val key = keyboardAdapter.getKey(position)
                    return when (key) {
                        // Bottom row: 123/ABC(1) 🎤(1) ␣(2) ←(1) →(1) ⌫(1) 🌐(1) = 8 spans!
                        "123", "ABC" -> 1  // Mode switch - reduced from 2 to 1
                        "\uD83C\uDF99" -> 1          // Microphone
                        "␣" -> 2           // Space
                        "←" -> 1           // Cursor left
                        "→" -> 1           // Cursor right
                        "⌫" -> 1           // Backspace
                        "🌐" -> 1          // Language
                        // For English: Z takes remaining 6 spaces
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
        cursorVisible = true  // Make cursor visible immediately when typing

        when (key) {
            "⌫" -> {
                // Backspace at cursor position
                if (cursorPosition > 0 && query.isNotEmpty()) {
                    query.deleteAt(cursorPosition - 1)
                    cursorPosition--
                }
            }
            "␣" -> {
                // Space at cursor position
                query.insert(cursorPosition, " ")
                cursorPosition++
            }
            "🌐" -> {
                toggleLanguage()
                return
            }
            "\uD83C\uDF99" -> {
                // Start voice search
                startVoiceSearch()
                return
            }
            "←" -> {
                // Move cursor left
                if (cursorPosition > 0) {
                    cursorPosition--
                }
                updateQueryDisplay()
                return
            }
            "→" -> {
                // Move cursor right
                if (cursorPosition < query.length) {
                    cursorPosition++
                }
                updateQueryDisplay()
                return
            }
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
            else -> {
                // Insert character at cursor position
                query.insert(cursorPosition, key)
                cursorPosition++
            }
        }

        updateQueryDisplay()
        updateResults()
    }

    private fun startVoiceSearch() {
        android.util.Log.d("SearchFragment", "Starting voice search")

        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "ru-RU")  // Default to Russian
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Скажите название приложения")
        }

        try {
            voiceSearchLauncher.launch(intent)
        } catch (e: Exception) {
            android.util.Log.e("SearchFragment", "Voice search not available", e)
            android.widget.Toast.makeText(
                requireContext(),
                "Голосовой поиск недоступен",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }

    private fun updateQueryDisplay() {
        // Show query with blinking cursor indicator
        val cursorChar = if (cursorVisible) "|" else " "
        val displayText = if (cursorPosition < query.length) {
            query.substring(0, cursorPosition) + cursorChar + query.substring(cursorPosition)
        } else {
            query.toString() + cursorChar
        }

        // Create spannable to color the cursor
        val spannable = android.text.SpannableString(displayText)
        if (cursorVisible && cursorPosition < displayText.length) {
            spannable.setSpan(
                android.text.style.ForegroundColorSpan(0xFF09E490.toInt()),  // Green cursor
                cursorPosition,
                cursorPosition + 1,
                android.text.Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        queryView.text = spannable
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
    private fun openSidebarAndFocus() {
        val mainActivity = activity as? MainActivity
        mainActivity?.openSidebar()

        view?.postDelayed({
            val sidebar = requireActivity().findViewById<View>(R.id.sidebar_container)
            val searchItem = sidebar?.findViewById<View>(R.id.menu_search)
            searchItem?.requestFocus()
        }, 50)
    }

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
                        "123","\uD83C\uDF99","␣","←","→","⌫","🌐"
                    )

                    KeyboardLang.EN -> listOf(
                        "A","B","C","D","E","F","G","H",
                        "I","J","K","L","M","N","O","P",
                        "Q","R","S","T","U","V","W","X",
                        "Y","Z",
                        "123","\uD83C\uDF99","␣","←","→","⌫","🌐"
                    )
                }

            KeyboardMode.NUMBERS -> listOf(
                "1","2","3","4","5","6","7","8",
                "9","0","-","_","+",
                ".",",",":",
                "ABC","\uD83C\uDF99","␣","←","→","⌫"
            )
        }
}
