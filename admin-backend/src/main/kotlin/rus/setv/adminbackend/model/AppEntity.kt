package rus.setv.adminbackend.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "apps")
class AppEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @Column(nullable = false)
    var name: String,

    @Column(nullable = false, unique = true)
    var packageName: String,

    var version: String? = null,

    @Column(columnDefinition = "TEXT")
    var description: String? = null,

    var iconUrl: String? = null,
    var bannerUrl: String? = null,
    var apkUrl: String? = null,

    var category: String? = null,

    @Enumerated(EnumType.STRING)
    var status: AppStatus = AppStatus.ACTIVE,

    var featured: Boolean = false,

    var updatedAt: LocalDateTime = LocalDateTime.now()
)
