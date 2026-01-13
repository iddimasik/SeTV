package rus.setv.adminbackend.repository

import org.springframework.data.jpa.repository.JpaRepository
import rus.setv.adminbackend.model.AppEntity
import java.util.*

interface AppRepository : JpaRepository<AppEntity, UUID>
