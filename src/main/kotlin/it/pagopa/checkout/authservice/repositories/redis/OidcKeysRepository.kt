package it.pagopa.checkout.authservice.repositories.redis

import it.pagopa.checkout.authservice.repositories.redis.bean.oidc.OidcKey
import java.time.Duration
import org.springframework.data.redis.core.ReactiveRedisTemplate

/** Authenticated user session repository */
class OidcKeysRepository(
    reactiveRedisTemplate: ReactiveRedisTemplate<String, OidcKey>,
    defaultTTL: Duration,
    keyspace: String,
) :
    ReactiveRedisTemplateWrapper<OidcKey>(
        reactiveRedisTemplate = reactiveRedisTemplate,
        ttl = defaultTTL,
        keyspace = keyspace,
    ) {
    public override fun getKeyFromEntity(value: OidcKey): String = value.kid
}
