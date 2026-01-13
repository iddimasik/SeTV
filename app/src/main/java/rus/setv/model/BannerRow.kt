package rus.setv.model

import androidx.leanback.widget.Row
import rus.setv.model.AppItem
import rus.setv.model.BannerItem

class BannerRow(
    val banners: List<BannerItem>,
    val recommendations: List<AppItem>
) : Row()
