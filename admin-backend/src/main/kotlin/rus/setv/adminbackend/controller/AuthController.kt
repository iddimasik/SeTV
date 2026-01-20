package rus.setv.adminbackend.controller

import rus.setv.adminbackend.dto.LoginRequest
import rus.setv.adminbackend.dto.LoginResponse
import rus.setv.adminbackend.service.JwtService
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val jwtService: JwtService,

    @Value("\${admin.login}")
    private val adminLogin: String,

    @Value("\${admin.password-hash}")
    private val adminPasswordHash: String
) {

    private val passwordEncoder = BCryptPasswordEncoder()

    @PostMapping("/login")
    fun login(
        @RequestBody request: LoginRequest
    ): ResponseEntity<LoginResponse> {

        val isLoginValid = request.login == adminLogin
        val isPasswordValid = passwordEncoder.matches(
            request.password,
            adminPasswordHash
        )

        if (!isLoginValid || !isPasswordValid) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build()
        }

        val token = jwtService.generateToken(request.login)

        return ResponseEntity.ok(
            LoginResponse(token = token)
        )
    }
}
