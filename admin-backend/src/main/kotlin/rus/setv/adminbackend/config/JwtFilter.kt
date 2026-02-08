package rus.setv.adminbackend.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.filter.OncePerRequestFilter
import rus.setv.adminbackend.service.JwtService

class JwtFilter(
    private val jwtService: JwtService
) : OncePerRequestFilter() {

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain
    ) {

        val path = request.requestURI

        if (
            path.startsWith("/api/public") ||
            path.startsWith("/api/auth") ||
            path.startsWith("/files/")
        ) {
            filterChain.doFilter(request, response)
            return
        }

        if (request.method.equals("OPTIONS", ignoreCase = true)) {
            filterChain.doFilter(request, response)
            return
        }

        val authHeader = request.getHeader("Authorization")

        if (authHeader.isNullOrBlank() || !authHeader.startsWith("Bearer ")) {
            response.status = HttpServletResponse.SC_FORBIDDEN
            return
        }

        val token = authHeader.removePrefix("Bearer ").trim()

        val username = jwtService.validateToken(token)
            ?: run {
                response.status = HttpServletResponse.SC_FORBIDDEN
                return
            }

        if (SecurityContextHolder.getContext().authentication == null) {
            val authentication = UsernamePasswordAuthenticationToken(
                username,
                null,
                listOf(SimpleGrantedAuthority("ROLE_ADMIN"))
            )
            SecurityContextHolder.getContext().authentication = authentication
        }

        filterChain.doFilter(request, response)
    }
}
