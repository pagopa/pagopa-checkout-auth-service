package test.authservice2.repositories.redis

import it.pagopa.checkout.authservice.repositories.redis.bean.oidc.OidcAuthStateData
import java.time.Duration
import org.springframework.data.redis.core.RedisTemplate

/** OIDC auth se */
class OIDCAuthStateDataRepository(
    redisTemplate: RedisTemplate<String, OidcAuthStateData>,
    defaultTTL: Duration,
    keyspace: String,
) :
    RedisTemplateWrapper<OidcAuthStateData>(
        redisTemplate = redisTemplate,
        ttl = defaultTTL,
        keyspace = keyspace,
    ) {
    override fun getKeyFromEntity(value: OidcAuthStateData) = value.state.value
}
