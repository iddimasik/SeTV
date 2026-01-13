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
    val bannerUrl: String?,
    val apkUrl: String?,
    val category: String?,
    val status: AppStatus,
    val featured: Boolean,
    val updatedAt: LocalDateTime
)

fun AppEntity.toDto() = AppDto(
    id,
    name,
    packageName,
    version,
    description,
    iconUrl,
    bannerUrl,
    apkUrl,
    category,
    status,
    featured,
    updatedAt
)
