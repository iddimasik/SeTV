package rus.setv.ui

import android.view.ViewGroup
import androidx.leanback.widget.Presenter
import rus.setv.model.AppItem

class RecommendedAppPresenter(
    private val onClick: (AppItem) -> Unit
) : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val view = RecommendedAppView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                dp(parent, 260),
                dp(parent, 60)
            )
        }
        return ViewHolder(view)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val app = item as AppItem
        val view = viewHolder.view as RecommendedAppView

        view.bind(app)
        view.onAppClick = onClick
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {}

    private fun dp(parent: ViewGroup, dp: Int): Int {
        return (dp * parent.resources.displayMetrics.density).toInt()
    }
}
