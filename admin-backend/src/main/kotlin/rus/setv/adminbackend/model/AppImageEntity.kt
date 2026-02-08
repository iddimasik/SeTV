package rus.setv.adminbackend.model

import jakarta.persistence.*
import java.time.LocalDateTime
import java.util.*

@Entity
@Table(name = "app_images")
class AppImageEntity(

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    val id: UUID? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "app_id", nullable = true)
    var app: AppEntity? = null,

    @Column(nullable = false)
    val imageUrl: String,

    @Column(nullable = false)
    var sortOrder: Int,

    val createdAt: LocalDateTime = LocalDateTime.now()
)
