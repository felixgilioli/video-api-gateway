package br.com.felixgilioli.videoapigateway.controller

import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * Controller de teste para validar autenticação JWT
 *
 * Endpoints úteis para debugar tokens e ver informações do usuário
 */
@RestController
@RequestMapping("/api")
class TestController {

    /**
     * Endpoint protegido que mostra informações do token JWT
     *
     * Requer: Authorization: Bearer <token>
     */
    @GetMapping("/me")
    fun me(@AuthenticationPrincipal jwt: Jwt): Map<String, Any?> {
        return mapOf(
            "subject" to jwt.subject,
            "email" to jwt.getClaim<String>("email"),
            "name" to jwt.getClaim<String>("name"),
            "preferred_username" to jwt.getClaim<String>("preferred_username"),
            "roles" to extractRoles(jwt),
            "token_issued_at" to jwt.issuedAt,
            "token_expires_at" to jwt.expiresAt,
            "issuer" to jwt.issuer
        )
    }

    /**
     * Endpoint simples protegido
     */
    @GetMapping("/hello")
    fun hello(@AuthenticationPrincipal jwt: Jwt): Map<String, String> {
        val username = jwt.getClaim<String>("preferred_username") ?: "unknown"
        return mapOf(
            "message" to "Hello, $username!",
            "authenticated" to "true"
        )
    }

    private fun extractRoles(jwt: Jwt): List<String> {
        val realmAccess = jwt.getClaim<Map<String, Any>>("realm_access") ?: return emptyList()

        @Suppress("UNCHECKED_CAST")
        return realmAccess["roles"] as? List<String> ?: emptyList()
    }
}