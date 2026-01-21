package rus.setv.data.network.api

import retrofit2.http.GET
import rus.setv.data.network.dto.AppDto

interface AppsApi {

    @GET("public/apps")
    suspend fun getPublicApps(): List<AppDto>
}
