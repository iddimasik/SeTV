package rus.setv.adminbackend.dto

import rus.setv.adminbackend.model.AppEntity
import rus.setv.adminbackend.model.AppStatus
import java.time.LocalDateTime
import java.util.*

data class AppDto(
    val id: UUID?,
    val name: String,
    val packageName: String,
    val version: String?,
    val description: String?,
    val iconUrl: String?,
    val apkUrl: String?,
    val category: String?,
    val status: AppStatus,
    val featured: Boolean,
    val updatedAt: LocalDateTime,
    val images: List<AppImageDto>
)

fun AppEntity.toDto(
    images: List<AppImageDto> = emptyList()
): AppDto =
    AppDto(
        id = id,
        name = name,
        packageName = packageName,
        version = version,
        description = description,
        iconUrl = iconUrl,
        apkUrl = apkUrl,
        category = category,
        status = status,
        featured = featured,
        updatedAt = updatedAt,
        images = images
    )