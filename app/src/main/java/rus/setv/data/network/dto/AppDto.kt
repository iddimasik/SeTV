package rus.setv.data.network.dto

data class AppDto(
    val name: String,
    val packageName: String,
    val version: String?,
    val description: String?,
    val iconUrl: String?,
    val bannerUrl: String?,
    val apkUrl: String?,
    val category: String?,
    val status: String,
    val featured: Boolean
)