package rus.setv.data.repository

import rus.setv.data.network.RetrofitClient
import rus.setv.model.AppImage
import rus.setv.model.AppItem

class AppsRepository {

    suspend fun loadApps(): Result<List<AppItem>> {
        return try {
            val apps = RetrofitClient.api.getPublicApps()
                .filter { it.apkUrl != null }
                .map { dto ->

                    AppItem(
                        name = dto.name,
                        packageName = dto.packageName,
                        apkUrl = dto.apkUrl!!,

                        description = dto.description,
                        iconUrl = dto.iconUrl,
                        version = dto.version,
                        featured = dto.featured,
                        category = dto.category,

                        images = dto.images
                            .sortedBy { it.sortOrder }
                            .map { img ->
                                AppImage(
                                    imageUrl = img.imageUrl,
                                    sortOrder = img.sortOrder
                                )
                            }
                    )
                }

            Result.success(apps)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
