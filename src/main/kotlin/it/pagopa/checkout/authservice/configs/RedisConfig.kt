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
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
class RedisConfig {

    @Bean
    @RegisterReflectionForBinding(AuthenticatedUserSession::class)
    fun authenticatedUserSessionRepository(
        redisConnectionFactory: ReactiveRedisConnectionFactory,
        @Value("\${authenticated-user-session.cache.ttlSeconds}") ttlSeconds: Long,
        @Value("\${authenticated-user-session.cache.keyspace}") keyspace: String,
    ): AuthenticatedUserSessionRepository {
        val jackson2JsonRedisSerializer =
            buildJackson2RedisSerializer(AuthenticatedUserSession::class.java)

        val context =
            RedisSerializationContext.newSerializationContext<String, AuthenticatedUserSession>(
                    StringRedisSerializer()
                )
                .value(jackson2JsonRedisSerializer)
                .build()

        val reactiveRedisTemplate = ReactiveRedisTemplate(redisConnectionFactory, context)

        return AuthenticatedUserSessionRepository(
            reactiveRedisTemplate = reactiveRedisTemplate,
            keyspace = keyspace,
            defaultTTL = Duration.ofSeconds(ttlSeconds),
        )
    }

    @Bean
    fun oidcAuthStateDataRepository(
        redisConnectionFactory: ReactiveRedisConnectionFactory,
        @Value("\${oidc.auth-state.cache.ttlSeconds}") ttlSeconds: Long,
        @Value("\${oidc.auth-state.cache.keyspace}") keyspace: String,
    ): OIDCAuthStateDataRepository {
        val keySerializer = StringRedisSerializer()
        val valueSerializer = buildJackson2RedisSerializer(OidcAuthStateData::class.java)

        val context =
            RedisSerializationContext.newSerializationContext<String, OidcAuthStateData>(
                    keySerializer
                )
                .value(valueSerializer)
                .build()

        val reactiveRedisTemplate = ReactiveRedisTemplate(redisConnectionFactory, context)

        return OIDCAuthStateDataRepository(
            reactiveRedisTemplate = reactiveRedisTemplate,
            defaultTTL = Duration.ofSeconds(ttlSeconds),
            keyspace = keyspace,
        )
    }

    @Bean
    @RegisterReflectionForBinding(OidcKey::class)
    fun oidcKeyRepository(
        redisConnectionFactory: ReactiveRedisConnectionFactory,
        @Value("\${oidc.keys.cache.ttlSeconds}") ttlSeconds: Long,
        @Value("\${oidc.keys.cache.keyspace}") keyspace: String,
    ): OidcKeysRepository {
        val keySerializer = StringRedisSerializer()
        val valueSerializer = buildJackson2RedisSerializer(OidcKey::class.java)

        val context =
            RedisSerializationContext.newSerializationContext<String, OidcKey>(keySerializer)
                .value(valueSerializer)
                .build()

        val reactiveRedisTemplate = ReactiveRedisTemplate(redisConnectionFactory, context)

        return OidcKeysRepository(
            reactiveRedisTemplate = reactiveRedisTemplate,
            defaultTTL = Duration.ofSeconds(ttlSeconds),
            keyspace = keyspace,
        )
    }

    private fun <T> buildJackson2RedisSerializer(clazz: Class<T>): Jackson2JsonRedisSerializer<T> {
        val jacksonObjectMapper = jacksonObjectMapper()
        return Jackson2JsonRedisSerializer(jacksonObjectMapper, clazz)
    }
}
