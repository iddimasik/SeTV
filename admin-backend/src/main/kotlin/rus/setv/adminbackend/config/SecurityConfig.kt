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
            .cors { }
            .csrf { it.disable() }

            .sessionManagement {
                it.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            }

            .authorizeHttpRequests {

                it.requestMatchers("/api/auth/**").permitAll()

                // ✅ ANDROID
                it.requestMatchers("/api/public/**").permitAll()

                // 🔒 АДМИНКА
                it.requestMatchers("/api/apps/**").hasRole("ADMIN")

                it.anyRequest().authenticated()
            }

            .addFilterBefore(
                JwtFilter(jwtService),
                UsernamePasswordAuthenticationFilter::class.java
            )

        return http.build()
    }

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val config = CorsConfiguration()
        config.allowedOriginPatterns = listOf("*")
        config.allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS")
        config.allowedHeaders = listOf("*")
        config.allowCredentials = false

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", config)
        return source
    }
}
