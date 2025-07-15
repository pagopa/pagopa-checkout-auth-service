package it.pagopa.checkout.authservice.repositories.redis

import it.pagopa.checkout.authservice.repositories.redis.bean.oidc.OidcAuthStateData
import java.time.Duration
import org.springframework.data.redis.core.ReactiveRedisTemplate

/** OIDC auth se */
class OIDCAuthStateDataRepository(
    reactiveRedisTemplate: ReactiveRedisTemplate<String, OidcAuthStateData>,
    defaultTTL: Duration,
    keyspace: String,
) :
    ReactiveRedisTemplateWrapper<OidcAuthStateData>(
        reactiveRedisTemplate = reactiveRedisTemplate,
        ttl = defaultTTL,
        keyspace = keyspace,
    ) {
    public override fun getKeyFromEntity(value: OidcAuthStateData): String {
        return value.state.value
    }
}
