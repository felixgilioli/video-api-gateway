package br.com.felixgilioli.videoapigateway.controller

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDateTime

/**
 * Controller de Health Check local
 * Não passa pelo roteamento do Gateway
 */
@RestController
class HealthController {

    @GetMapping("/health")
    fun health() = mapOf(
        "status" to "UP",
        "service" to "video-api-gateway",
        "timestamp" to LocalDateTime.now(),
        "message" to "Gateway is running! 🚀"
    )
}