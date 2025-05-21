package it.pagopa.checkout.authservice.repositories.redis

import it.pagopa.checkout.authservice.repositories.redis.bean.auth.AuthenticatedUserSession
import java.time.Duration
import org.springframework.data.redis.core.ReactiveRedisTemplate

/** Authenticated user session repository */
class AuthenticatedUserSessionRepository(
    reactiveRedisTemplate: ReactiveRedisTemplate<String, AuthenticatedUserSession>,
    defaultTTL: Duration,
    keyspace: String,
) :
    ReactiveRedisTemplateWrapper<AuthenticatedUserSession>(
        ttl = defaultTTL,
        keyspace = keyspace,
        reactiveRedisTemplate = reactiveRedisTemplate,
    ) {
    public override fun getKeyFromEntity(value: AuthenticatedUserSession) = value.sessionToken.value
}
