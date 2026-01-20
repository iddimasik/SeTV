package rus.setv.adminbackend.config

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource
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

        val authHeader = request.getHeader("Authorization")

        if (authHeader != null && authHeader.startsWith("Bearer ")) {

            val token = authHeader.substring(7)
            val username = jwtService.validateToken(token)

            if (username != null &&
                SecurityContextHolder.getContext().authentication == null
            ) {

                val authorities = listOf(
                    SimpleGrantedAuthority("ROLE_ADMIN")
                )

                val authentication = UsernamePasswordAuthenticationToken(
                    username,
                    null,
                    authorities
                )

                authentication.details =
                    WebAuthenticationDetailsSource().buildDetails(request)

                SecurityContextHolder
                    .getContext()
                    .authentication = authentication
            }
        }

        filterChain.doFilter(request, response)
    }
}
