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
            // CORS
            .cors { }

            // CSRF выключаем (JWT + REST)
            .csrf { it.disable() }

            // Stateless (JWT)
            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }

            // Авторизация
            .authorizeHttpRequests {

                // 🔓 Авторизация
                it.requestMatchers("/api/auth/**").permitAll()

                // 🔒 Все API админки (apps, parse-apk и т.д.)
                it.requestMatchers("/api/apps/**").hasRole("ADMIN")

                // 🔒 Swagger
                it.requestMatchers(
                    "/swagger",
                    "/swagger/**",
                    "/v3/api-docs/**",
                    "/swagger-ui/**",
                    "/swagger-ui.html"
                ).hasRole("ADMIN")

                // 🔒 Всё остальное
                it.anyRequest().authenticated()
            }

            // JWT Filter
            .addFilterBefore(
                JwtFilter(jwtService),
                UsernamePasswordAuthenticationFilter::class.java
            )

        return http.build()
    }

    /**
     * Глобальный CORS
     */
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
