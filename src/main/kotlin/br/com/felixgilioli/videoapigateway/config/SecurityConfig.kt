package br.com.felixgilioli.videoapigateway.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.web.server.SecurityWebFilterChain

@Configuration
@EnableWebFluxSecurity
class SecurityConfig {

    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .csrf { it.disable() }  // Desabilita CSRF (API REST não precisa)
            .authorizeExchange { exchanges ->
                exchanges
                    .pathMatchers("/actuator/**").permitAll()  // Health checks públicos
                    .pathMatchers("/test/**").permitAll()      // Rota de teste pública
                    .anyExchange().permitAll()                 // Trocar por authenticated() depois
            }
            .build()
    }
}
