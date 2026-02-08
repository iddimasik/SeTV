package rus.setv.adminbackend.repository

import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.transaction.annotation.Transactional
import rus.setv.adminbackend.model.AppImageEntity
import java.util.*

interface AppImageRepository : JpaRepository<AppImageEntity, UUID> {

    fun findAllByAppIdOrderBySortOrder(appId: UUID): List<AppImageEntity>

    @Transactional
    fun deleteAllByAppId(appId: UUID)
}
