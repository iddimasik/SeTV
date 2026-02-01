package rus.setv

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

    inner class KeyViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val text: TextView = view.findViewById(R.id.keyText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): KeyViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_keyboard_key, parent, false)

        view.isFocusable = true
        view.isFocusableInTouchMode = true

        return KeyViewHolder(view)
    }

    override fun onBindViewHolder(holder: KeyViewHolder, position: Int) {
        val key = keys[position]

        holder.text.text = key

        // ───── ШИРИНА СПЕЦ-КНОПОК ─────
        val params = holder.itemView.layoutParams
        params.width = when (key) {
            "␣" -> dpToPx(holder.itemView, 90)  // пробел шире
            "⌫", "🌐", "123", "ABC" -> dpToPx(holder.itemView, 60)
            else -> dpToPx(holder.itemView, 40)   // обычные буквы
        }
        holder.itemView.layoutParams = params

        // ───── КЛИК ─────
        holder.itemView.setOnClickListener {
            onClick(key)
        }

        // ───── ПОДСВЕТКА ФОКУСА ─────
        holder.itemView.setOnFocusChangeListener { v, hasFocus ->
            v.alpha = if (hasFocus) 1f else 0.7f
            holder.text.isSelected = hasFocus
        }
    }

    override fun getItemCount(): Int = keys.size

    fun setKeys(newKeys: List<String>) {
        keys = newKeys
        notifyDataSetChanged()
    }

    fun getKey(position: Int): String = keys[position]

    private fun dpToPx(view: View, dp: Int): Int =
        (dp * view.resources.displayMetrics.density).toInt()
}
