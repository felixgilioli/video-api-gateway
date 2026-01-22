package br.com.felixgilioli.videoapigateway

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.data.redis.autoconfigure.DataRedisAutoConfiguration
import org.springframework.boot.data.redis.autoconfigure.DataRedisReactiveAutoConfiguration
import org.springframework.boot.runApplication

@SpringBootApplication(exclude = [
	DataRedisAutoConfiguration::class,
	DataRedisReactiveAutoConfiguration::class
])
class VideoApiGatewayApplication

fun main(args: Array<String>) {
	runApplication<VideoApiGatewayApplication>(*args)
}
