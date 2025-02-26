package it.pagopa.checkout.authservice.repositories.redis

import it.pagopa.checkout.authservice.repositories.redis.bean.oidc.OidcKey
import java.time.Duration
import org.springframework.data.redis.core.RedisTemplate

/** Authenticated user session repository */
class OidcKeysRepository(redisTemplate: RedisTemplate<String, OidcKey>, defaultTTL: Duration) :
    RedisTemplateWrapper<OidcKey>(
        ttl = defaultTTL,
        keyspace = "oidc-keys",
        redisTemplate = redisTemplate,
    ) {
    override fun getKeyFromEntity(value: OidcKey) = value.kid
}
