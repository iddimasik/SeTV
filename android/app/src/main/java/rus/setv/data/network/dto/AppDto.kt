package rus.setv.data.network.dto

import java.util.UUID

data class AppDto(
    val id: UUID,

    val name: String,
    val packageName: String,

    val version: String?,
    val description: String?,

    val iconUrl: String?,
    val apkUrl: String?,

    val category: String?,

    val status: String,
    val featured: Boolean,

    val images: List<AppImageDto> = emptyList()
)
