package it.pagopa.checkout.authservice.repositories.redis

import it.pagopa.checkout.authservice.repositories.redis.bean.oidc.OidcAuthStateData
import java.time.Duration
import org.springframework.data.redis.core.RedisTemplate

/** OIDC auth se */
class OIDCAuthStateDataRepository(
    redisTemplate: RedisTemplate<String, OidcAuthStateData>,
    defaultTTL: Duration,
) :
    RedisTemplateWrapper<OidcAuthStateData>(
        redisTemplate = redisTemplate,
        ttl = defaultTTL,
        keyspace = "oidc-auth-session-data",
    ) {
    override fun getKeyFromEntity(value: OidcAuthStateData) = value.state.value
}
