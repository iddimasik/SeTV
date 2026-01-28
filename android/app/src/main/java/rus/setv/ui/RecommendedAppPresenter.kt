package rus.setv.ui

import android.view.ViewGroup
import androidx.leanback.widget.Presenter
import rus.setv.model.AppItem

class RecommendedAppPresenter(
    private val onClick: (AppItem) -> Unit
) : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val view = RecommendedAppView(parent.context).apply {
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
}
