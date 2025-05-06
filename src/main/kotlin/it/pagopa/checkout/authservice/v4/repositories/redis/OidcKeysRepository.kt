package it.pagopa.checkout.authservice.v4.repositories.redis

import it.pagopa.checkout.authservice.v4.repositories.redis.bean.oidc.OidcKey
import java.time.Duration
import org.springframework.data.redis.core.RedisTemplate

/** Authenticated user session repository */
class OidcKeysRepository(
    redisTemplate: RedisTemplate<String, OidcKey>,
    defaultTTL: Duration,
    keyspace: String,
) :
    RedisTemplateWrapper<OidcKey>(
        ttl = defaultTTL,
        keyspace = keyspace,
        redisTemplate = redisTemplate,
    ) {
    override fun getKeyFromEntity(value: OidcKey) = value.kid
}
