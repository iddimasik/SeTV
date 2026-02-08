package rus.setv.ui

import android.view.KeyEvent
import android.view.ViewGroup
import androidx.leanback.widget.Presenter
import rus.setv.model.AppItem
import rus.setv.R

class RecommendedAppPresenter(
    private val onClick: (AppItem) -> Unit
) : Presenter() {

    var onLastItemNavigateDown: (() -> Unit)? = null
    var isLastItemProvider: (() -> Boolean)? = null

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val view = RecommendedAppView(parent.context)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val app = item as AppItem
        val view = viewHolder.view as RecommendedAppView

        view.bind(app)
        view.onAppClick = onClick

        val root = view.findViewById<android.view.View>(R.id.recommendedRoot)
        root?.setOnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN &&
                event.action == KeyEvent.ACTION_DOWN &&
                isLastItemProvider?.invoke() == true) {
                onLastItemNavigateDown?.invoke()
                true
            } else {
                false
            }
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {}
}
