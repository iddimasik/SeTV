package rus.setv.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AppItem(

    val name: String,
    val packageName: String,
    val apkUrl: String,

    val description: String? = null,
    val iconUrl: String? = null,
    val version: String? = null,
    val featured: Boolean = false,

    val category: String? = null,

    val images: List<AppImage> = emptyList(),

    var status: AppStatus = AppStatus.NOT_INSTALLED,
    var progress: Int = 0,

    var apkSizeBytes: Long = 0L
) : Parcelable
