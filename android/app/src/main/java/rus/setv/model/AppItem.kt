package rus.setv.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AppItem(
    // ───── данные с сервера ─────
    val name: String,
    val packageName: String,
    val apkUrl: String,

    val description: String? = null,
    val iconUrl: String? = null,
    val version: String? = null,
    val featured: Boolean = false,

    // ───── состояние на устройстве ─────
    var status: AppStatus = AppStatus.NOT_INSTALLED,
    var progress: Int = 0
) : Parcelable
