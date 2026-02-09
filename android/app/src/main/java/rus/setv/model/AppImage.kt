package rus.setv.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class AppImage(
    val imageUrl: String,
    val sortOrder: Int
) : Parcelable
