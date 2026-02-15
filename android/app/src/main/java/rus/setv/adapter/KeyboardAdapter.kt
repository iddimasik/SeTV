package rus.setv.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import rus.setv.R

class KeyboardAdapter(
    private var keys: List<String>,
    private val onClick: (String) -> Unit
) : RecyclerView.Adapter<KeyboardAdapter.KeyViewHolder>() {

    private var recyclerView: RecyclerView? = null
    var onLeftKeyFromFirstColumn: (() -> Unit)? = null
    var hasResultsProvider: (() -> Boolean)? = null  // Check if search has results

    // ─────────────────────────────
    // VIEW HOLDER
    // ─────────────────────────────
    inner class KeyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val text: TextView = view.findViewById(R.id.keyText)
    }

    // ─────────────────────────────
    // ATTACH
    // ─────────────────────────────
    override fun onAttachedToRecyclerView(rv: RecyclerView) {
        super.onAttachedToRecyclerView(rv)
        recyclerView = rv
    }

    override fun onDetachedFromRecyclerView(rv: RecyclerView) {
        super.onDetachedFromRecyclerView(rv)
        recyclerView = null
    }

    // ─────────────────────────────
    // CREATE
    // ─────────────────────────────
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KeyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_keyboard_key, parent, false)

        view.isFocusable = true
        view.isFocusableInTouchMode = true

        return KeyViewHolder(view)
    }

    // ─────────────────────────────
    // BIND
    // ─────────────────────────────
    override fun onBindViewHolder(holder: KeyViewHolder, position: Int) {
        if (position == RecyclerView.NO_POSITION) return

        val key = keys[position]
        holder.text.text = key

        // Hide empty keys
        if (key.isEmpty()) {
            holder.itemView.visibility = android.view.View.INVISIBLE
            holder.itemView.isFocusable = false
            return
        } else {
            holder.itemView.visibility = android.view.View.VISIBLE
            holder.itemView.isFocusable = true
        }

        // ───── WIDTH ─────
        val params = holder.itemView.layoutParams
        params.width = when (key) {
            "␣" -> dpToPx(holder.itemView, 100)   // Space - 2 spans
            "123", "ABC" -> dpToPx(holder.itemView, 50)  // Mode switches - 1 span (reduced!)
            "←", "→", "⌫", "🌐", "🎤" -> dpToPx(holder.itemView, 50)  // Single span buttons
            else -> dpToPx(holder.itemView, 50)   // Regular keys - 1 span
        }
        holder.itemView.layoutParams = params

        // ───── CLICK ─────
        holder.itemView.setOnClickListener {
            if (holder.bindingAdapterPosition != RecyclerView.NO_POSITION) {
                onClick(key)
            }
        }

        // Disable pressed state to prevent trail effect
        holder.itemView.isClickable = true
        holder.itemView.isFocusable = true
        holder.itemView.isFocusableInTouchMode = true

        // ───── LEFT KEY (first column only) ─────
        val isFirstColumn = position % 8 == 0

        // ───── DOWN KEY (last row only) ─────
        // Calculate last row based on total number of keys
        // RU letters: 32 letters + 7 control = 39, last row starts at 32
        // EN letters: 26 letters + 7 control = 33, last row starts at 26
        // Numbers: 16 numbers + 6 control = 22, last row starts at 17
        val totalKeys = itemCount
        val isLastRow = when {
            totalKeys == 39 -> position >= 32  // Russian letters
            totalKeys == 33 -> position >= 26  // English letters
            totalKeys == 22 -> position >= 17  // Numbers
            else -> false
        }

        if (isFirstColumn || isLastRow) {
            holder.itemView.setOnKeyListener { _, keyCode, event ->
                when {
                    keyCode == android.view.KeyEvent.KEYCODE_DPAD_LEFT &&
                            event.action == android.view.KeyEvent.ACTION_DOWN && isFirstColumn -> {
                        onLeftKeyFromFirstColumn?.invoke()
                        true
                    }
                    keyCode == android.view.KeyEvent.KEYCODE_DPAD_DOWN &&
                            event.action == android.view.KeyEvent.ACTION_DOWN && isLastRow -> {
                        // Block DOWN if no results
                        val hasResults = hasResultsProvider?.invoke() ?: true
                        !hasResults  // Return true to consume event (block), false to allow
                    }
                    else -> false
                }
            }
        } else {
            holder.itemView.setOnKeyListener(null)
        }

        // ───── FOCUS ─────
        holder.itemView.setOnFocusChangeListener { v, hasFocus ->
            holder.text.isSelected = hasFocus

            if (hasFocus) {
                v.alpha = 1f
            } else {
                // RADICAL: Completely recreate background to remove ALL states
                v.alpha = 1f
                v.isPressed = false
                v.isSelected = false
                v.isActivated = false
                v.clearAnimation()

                // Force background recreation
                v.background = null
                v.setBackgroundResource(R.drawable.bg_keyboard_key)
                v.refreshDrawableState()
            }
        }
    }

    override fun getItemCount(): Int = keys.size

    // ─────────────────────────────
    // UPDATE KEYS
    // ─────────────────────────────
    fun setKeys(newKeys: List<String>) {
        keys = newKeys
        notifyDataSetChanged()

        // вернуть фокус на первую клавишу
        recyclerView?.post {
            if (itemCount > 0) {
                recyclerView
                    ?.findViewHolderForAdapterPosition(0)
                    ?.itemView
                    ?.requestFocus()
            }
        }
    }

    fun getKey(position: Int): String =
        keys.getOrNull(position) ?: ""

    // ─────────────────────────────
    // UTILS
    // ─────────────────────────────
    private fun dpToPx(view: View, dp: Int): Int =
        (dp * view.resources.displayMetrics.density).toInt()
}