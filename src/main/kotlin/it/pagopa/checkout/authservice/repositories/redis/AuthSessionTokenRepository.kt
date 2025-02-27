package it.pagopa.checkout.authservice.repositories.redis

import it.pagopa.checkout.authservice.repositories.redis.bean.oidc.AuthSessionToken
import java.time.Duration
import org.springframework.data.redis.core.RedisTemplate

/** Authentication to session token repository implementation */
class AuthSessionTokenRepository(
    redisTemplate: RedisTemplate<String, AuthSessionToken>,
    defaultTtl: Duration,
) :
    RedisTemplateWrapper<AuthSessionToken>(
        redisTemplate = redisTemplate,
        ttl = defaultTtl,
        keyspace = "auth-session-token",
    ) {
    override fun getKeyFromEntity(value: AuthSessionToken) = value.authCode.value
}
