package rus.setv.adminbackend.config

import rus.setv.adminbackend.service.JwtService
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

@Configuration
class SecurityConfig(
    private val jwtService: JwtService
) {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {

        http
            // 🔓 CORS (Android, Web, nginx)
            .cors { }

            // ❌ CSRF не нужен для JWT
            .csrf { it.disable() }

            // 🚫 Без сессий
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }

            .authorizeHttpRequests {

                // 🔓 PUBLIC API (Android TV)
                it.requestMatchers("/api/public/**").permitAll()

                // 🔓 AUTH
                it.requestMatchers("/api/auth/**").permitAll()

                // 🔒 ADMIN API
                it.requestMatchers("/api/apps/**").hasRole("ADMIN")

                // ❌ всё остальное — запрещено
                it.anyRequest().denyAll()
            }

            // 🔐 JWT фильтр
            .addFilterBefore(
                JwtFilter(jwtService),
                UsernamePasswordAuthenticationFilter::class.java
            )

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()

        configuration.allowedOriginPatterns = listOf("*")
        configuration.allowedMethods = listOf(
            "GET",
            "POST",
            "PUT",
            "DELETE",
            "OPTIONS"
        )
        configuration.allowedHeaders = listOf("*")
        configuration.allowCredentials = false

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)

        return source
    }
}
