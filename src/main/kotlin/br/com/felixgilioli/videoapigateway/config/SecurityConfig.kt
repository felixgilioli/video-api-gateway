package br.com.felixgilioli.videoapigateway.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.convert.converter.Converter
import org.springframework.security.authentication.AbstractAuthenticationToken
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity
import org.springframework.security.config.web.server.ServerHttpSecurity
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter
import org.springframework.security.web.server.SecurityWebFilterChain
import reactor.core.publisher.Mono

/**
 * Configuração de Segurança do API Gateway
 *
 * Integrado com Keycloak via OAuth2 Resource Server
 * - Valida tokens JWT automaticamente
 * - Extrai roles do token (realm_access.roles)
 * - Define rotas públicas e protegidas
 */
@Configuration
@EnableWebFluxSecurity
class SecurityConfig {

    @Bean
    fun securityWebFilterChain(http: ServerHttpSecurity): SecurityWebFilterChain {
        return http
            .csrf { it.disable() }  // Desabilita CSRF (API REST não precisa)
            .authorizeExchange { exchanges ->
                exchanges
                    // Rotas PÚBLICAS (sem autenticação)
                    .pathMatchers("/actuator/**").permitAll()
                    .pathMatchers("/health").permitAll()
                    .pathMatchers("/test/**").permitAll()

                    // Rotas PROTEGIDAS (requerem autenticação)
                    .pathMatchers("/protected/**").authenticated()

                    // Todas as outras rotas requerem autenticação
                    .anyExchange().authenticated()
            }
            .oauth2ResourceServer { oauth2 ->
                oauth2.jwt { jwt ->
                    jwt.jwtAuthenticationConverter(grantedAuthoritiesExtractor())
                }
            }
            .build()
    }

    /**
     * Extrai as roles do token JWT do Keycloak
     *
     * O Keycloak armazena roles em: realm_access.roles
     * Exemplo de token:
     * {
     *   "realm_access": {
     *     "roles": ["user", "admin"]
     *   }
     * }
     */
    @Bean
    fun grantedAuthoritiesExtractor(): Converter<Jwt, Mono<AbstractAuthenticationToken>> {
        val jwtAuthenticationConverter = JwtAuthenticationConverter()
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(KeycloakRoleConverter())
        return ReactiveJwtAuthenticationConverterAdapter(jwtAuthenticationConverter)
    }

    /**
     * Converter customizado para extrair roles do Keycloak JWT
     */
    class KeycloakRoleConverter : Converter<Jwt, Collection<GrantedAuthority>> {

        override fun convert(jwt: Jwt): Collection<GrantedAuthority> {
            // Extrai realm_access.roles do token
            val realmAccess = jwt.getClaim<Map<String, Any>>("realm_access")

            if (realmAccess == null || !realmAccess.containsKey("roles")) {
                return emptyList()
            }

            @Suppress("UNCHECKED_CAST")
            val roles = realmAccess["roles"] as? List<String> ?: emptyList()

            // Converte roles para GrantedAuthority com prefixo ROLE_
            return roles.map { role ->
                SimpleGrantedAuthority("ROLE_${role.uppercase()}")
            }
        }
    }
}