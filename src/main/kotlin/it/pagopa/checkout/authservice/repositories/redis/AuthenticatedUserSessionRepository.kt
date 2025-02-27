package it.pagopa.checkout.authservice.repositories.redis

import it.pagopa.checkout.authservice.repositories.redis.bean.auth.AuthenticatedUserSession
import java.time.Duration
import org.springframework.data.redis.core.RedisTemplate

/** Authenticated user session repository */
class AuthenticatedUserSessionRepository(
    redisTemplate: RedisTemplate<String, AuthenticatedUserSession>,
    defaultTTL: Duration,
) :
    RedisTemplateWrapper<AuthenticatedUserSession>(
        ttl = defaultTTL,
        keyspace = "authenticated-user-session",
        redisTemplate = redisTemplate,
    ) {
    override fun getKeyFromEntity(value: AuthenticatedUserSession) =
        value.sessionToken.sessionToken.toString()
}
