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

        // ───── WIDTH ─────
        val params = holder.itemView.layoutParams
        params.width = when (key) {
            "␣" -> dpToPx(holder.itemView, 90)
            "⌫", "🌐", "123", "ABC" -> dpToPx(holder.itemView, 60)
            else -> dpToPx(holder.itemView, 40)
        }
        holder.itemView.layoutParams = params

        // ───── CLICK ─────
        holder.itemView.setOnClickListener {
            if (holder.bindingAdapterPosition != RecyclerView.NO_POSITION) {
                onClick(key)
            }
        }

        // ───── FOCUS ─────
        holder.itemView.setOnFocusChangeListener { v, hasFocus ->
            v.alpha = if (hasFocus) 1f else 0.6f
            holder.text.isSelected = hasFocus
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