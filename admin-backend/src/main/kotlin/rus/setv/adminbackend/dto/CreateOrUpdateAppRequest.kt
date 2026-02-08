package rus.setv.adminbackend.dto

import rus.setv.adminbackend.model.AppStatus

data class AppRequest(
    val name: String,
    val packageName: String,
    val version: String?,
    val description: String?,
    val iconUrl: String?,
    val apkUrl: String?,
    val category: String?,
    val status: AppStatus,
    val featured: Boolean,
    val images: List<AppImageDto> = emptyList()
)
