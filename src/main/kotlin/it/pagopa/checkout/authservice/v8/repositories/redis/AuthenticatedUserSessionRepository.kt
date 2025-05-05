package it.pagopa.checkout.authservice.v8.repositories.redis

import it.pagopa.checkout.authservice.v8.repositories.redis.bean.auth.AuthenticatedUserSession
import java.time.Duration
import org.springframework.data.redis.core.RedisTemplate

/** Authenticated user session repository */
class AuthenticatedUserSessionRepository(
    redisTemplate: RedisTemplate<String, AuthenticatedUserSession>,
    defaultTTL: Duration,
    keyspace: String,
) :
    RedisTemplateWrapper<AuthenticatedUserSession>(
        ttl = defaultTTL,
        keyspace = keyspace,
        redisTemplate = redisTemplate,
    ) {
    override fun getKeyFromEntity(value: AuthenticatedUserSession) = value.sessionToken.value
}
