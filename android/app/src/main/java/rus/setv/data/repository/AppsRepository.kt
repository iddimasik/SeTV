package rus.setv.data.repository

import rus.setv.data.network.RetrofitClient
import rus.setv.model.AppItem

class AppsRepository {

    suspend fun loadApps(): List<AppItem> {

        return RetrofitClient.api.getPublicApps()
            .filter { it.apkUrl != null }
            .map { dto ->
                AppItem(
                    name = dto.name,
                    description = dto.description,
                    packageName = dto.packageName,
                    apkUrl = dto.apkUrl!!,
                    iconUrl = dto.iconUrl,
                    version = dto.version,
                    featured = dto.featured,
                    category = dto.category
                )
            }
    }
}
