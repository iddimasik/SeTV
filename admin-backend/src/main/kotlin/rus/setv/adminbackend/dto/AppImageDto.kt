package rus.setv.adminbackend.dto

import java.util.*

data class AppImageDto(
    val id: UUID? = null,
    val imageUrl: String,
    var sortOrder: Int = 0
)
