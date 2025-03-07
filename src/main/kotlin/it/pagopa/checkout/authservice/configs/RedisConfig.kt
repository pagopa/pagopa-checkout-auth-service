package it.pagopa.checkout.authservice.configs

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import it.pagopa.checkout.authservice.repositories.redis.AuthenticatedUserSessionRepository
import it.pagopa.checkout.authservice.repositories.redis.OIDCAuthStateDataRepository
import it.pagopa.checkout.authservice.repositories.redis.OidcKeysRepository
import it.pagopa.checkout.authservice.repositories.redis.bean.auth.AuthenticatedUserSession
import it.pagopa.checkout.authservice.repositories.redis.bean.oidc.OidcAuthStateData
import it.pagopa.checkout.authservice.repositories.redis.bean.oidc.OidcKey
import java.time.Duration
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
class RedisConfig {

    @Bean
    @RegisterReflectionForBinding(AuthenticatedUserSession::class)
    fun authenticatedUserSessionRepository(
        redisConnectionFactory: RedisConnectionFactory,
        @Value("\${authenticated-user-session.cache.ttlSeconds}") ttlSeconds: Long,
    ): AuthenticatedUserSessionRepository {
        val redisTemplate = RedisTemplate<String, AuthenticatedUserSession>()
        redisTemplate.connectionFactory = redisConnectionFactory
        val jackson2JsonRedisSerializer =
            buildJackson2RedisSerializer(AuthenticatedUserSession::class.java)
        redisTemplate.valueSerializer = jackson2JsonRedisSerializer
        redisTemplate.keySerializer = StringRedisSerializer()
        redisTemplate.afterPropertiesSet()
        return AuthenticatedUserSessionRepository(redisTemplate, Duration.ofSeconds(ttlSeconds))
    }

    @Bean
    @RegisterReflectionForBinding(OidcAuthStateData::class)
    fun oidcAuthStateRepository(
        redisConnectionFactory: RedisConnectionFactory,
        @Value("\${oidc.auth-state.cache.ttlSeconds}") ttlSeconds: Long,
    ): OIDCAuthStateDataRepository {
        val redisTemplate = RedisTemplate<String, OidcAuthStateData>()
        redisTemplate.connectionFactory = redisConnectionFactory
        val jackson2JsonRedisSerializer =
            buildJackson2RedisSerializer(OidcAuthStateData::class.java)
        redisTemplate.valueSerializer = jackson2JsonRedisSerializer
        redisTemplate.keySerializer = StringRedisSerializer()
        redisTemplate.afterPropertiesSet()
        return OIDCAuthStateDataRepository(redisTemplate, Duration.ofSeconds(ttlSeconds))
    }

    @Bean
    @RegisterReflectionForBinding(OidcKey::class)
    fun oidcKeyRepository(
        redisConnectionFactory: RedisConnectionFactory,
        @Value("\${oidc.keys.cache.ttlSeconds}") ttlSeconds: Long,
    ): OidcKeysRepository {
        val redisTemplate = RedisTemplate<String, OidcKey>()
        redisTemplate.connectionFactory = redisConnectionFactory
        val jackson2JsonRedisSerializer = buildJackson2RedisSerializer(OidcKey::class.java)
        redisTemplate.valueSerializer = jackson2JsonRedisSerializer
        redisTemplate.keySerializer = StringRedisSerializer()
        redisTemplate.afterPropertiesSet()
        return OidcKeysRepository(redisTemplate, Duration.ofSeconds(ttlSeconds))
    }

    private fun <T> buildJackson2RedisSerializer(clazz: Class<T>): Jackson2JsonRedisSerializer<T> {
        val jacksonObjectMapper = jacksonObjectMapper()
        return Jackson2JsonRedisSerializer(jacksonObjectMapper, clazz)
    }
}
