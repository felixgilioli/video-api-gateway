package br.com.felixgilioli.videoapigateway.filter

import org.springframework.cloud.gateway.filter.GatewayFilterChain
import org.springframework.cloud.gateway.filter.GlobalFilter
import org.springframework.core.Ordered
import org.springframework.security.core.context.ReactiveSecurityContextHolder
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken
import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

/**
 * Filtro Global que extrai informações do JWT e adiciona como headers
 * para os serviços downstream.
 *
 * Headers adicionados:
 * - X-User-Id: Subject do token (ID do usuário no Keycloak)
 * - X-User-Email: Email do usuário
 * - X-User-Name: Nome completo do usuário
 * - X-User-Username: Username (preferred_username)
 * - X-User-Roles: Roles do usuário (separadas por vírgula)
 *
 * Os serviços downstream podem simplesmente ler esses headers
 * sem precisar validar o JWT novamente.
 */
@Component
class UserInfoFilter : GlobalFilter, Ordered {

    override fun filter(exchange: ServerWebExchange, chain: GatewayFilterChain): Mono<Void> {
        return ReactiveSecurityContextHolder.getContext()
            .map { it.authentication as Any }
            .filter { it is JwtAuthenticationToken }
            .cast(JwtAuthenticationToken::class.java)
            .map { jwtAuth -> buildModifiedExchange(exchange, jwtAuth.token) }
            .defaultIfEmpty(exchange)
            .flatMap { chain.filter(it) }
    }

    private fun buildModifiedExchange(exchange: ServerWebExchange, jwt: Jwt): ServerWebExchange {
        val modifiedRequest = exchange.request.mutate()
            .header("X-User-Id", extractUserId(jwt))
            .header("X-User-Email", extractEmail(jwt))
            .header("X-User-Name", extractName(jwt))
            .header("X-User-Username", extractUsername(jwt))
            .header("X-User-Roles", extractRoles(jwt))
            .build()

        return exchange.mutate().request(modifiedRequest).build()
    }

    /**
     * Ordem de execução do filtro
     * Valor mais baixo = executa primeiro
     * -1 = executa após autenticação mas antes de outros filtros
     */
    override fun getOrder(): Int = -1

    /**
     * Extrai o User ID (subject) do token
     */
    private fun extractUserId(jwt: Jwt): String {
        return jwt.subject ?: "unknown"
    }

    /**
     * Extrai o email do token
     */
    private fun extractEmail(jwt: Jwt): String {
        return jwt.getClaim<String>("email") ?: ""
    }

    /**
     * Extrai o nome completo do token
     */
    private fun extractName(jwt: Jwt): String {
        return jwt.getClaim<String>("name") ?: ""
    }

    /**
     * Extrai o username (preferred_username) do token
     */
    private fun extractUsername(jwt: Jwt): String {
        return jwt.getClaim<String>("preferred_username") ?: ""
    }

    /**
     * Extrai as roles do token Keycloak
     * Formato no token: { "realm_access": { "roles": ["user", "admin"] } }
     * Retorna: "user,admin"
     */
    private fun extractRoles(jwt: Jwt): String {
        val realmAccess = jwt.getClaim<Map<String, Any>>("realm_access")
            ?: return ""

        @Suppress("UNCHECKED_CAST")
        val roles = realmAccess["roles"] as? List<String>
            ?: return ""

        return roles.joinToString(",")
    }
}