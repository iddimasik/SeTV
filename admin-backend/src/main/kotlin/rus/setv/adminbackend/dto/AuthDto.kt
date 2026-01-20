package rus.setv.adminbackend.dto

data class LoginRequest(
    val login: String,
    val password: String
)

data class LoginResponse(
    val token: String
)