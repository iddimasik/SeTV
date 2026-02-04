package rus.setv.data.repository

import rus.setv.data.network.RetrofitClient
import rus.setv.model.AppItem
import java.io.IOException
import java.net.SocketTimeoutException

class AppsRepository {

    suspend fun loadApps(): Result<List<AppItem>> {
        return try {
            val apps = RetrofitClient.api.getPublicApps()
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

            Result.success(apps)

        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
