package rus.setv.data.network.dto

import java.util.UUID

data class AppImageDto(
    val id: UUID,
    val imageUrl: String,
    val sortOrder: Int
)
