package br.com.felixgilioli.videoapigateway.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.reactive.CorsWebFilter
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource

@Configuration
class CorsConfig {

    @Bean
    @Order(-1)
    fun corsWebFilter(): CorsWebFilter {
        val corsConfiguration = CorsConfiguration().apply {

            allowedOrigins = listOf("http://localhost:4200")
            allowedMethods = listOf("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH")
            allowedHeaders = listOf("*")

            // Permitir credentials (cookies, authorization headers)
            allowCredentials = true

            // Tempo de cache do preflight (OPTIONS)
            maxAge = 3600L
        }

        val source = UrlBasedCorsConfigurationSource().apply {
            registerCorsConfiguration("/**", corsConfiguration)
        }

        return CorsWebFilter(source)
    }
}
