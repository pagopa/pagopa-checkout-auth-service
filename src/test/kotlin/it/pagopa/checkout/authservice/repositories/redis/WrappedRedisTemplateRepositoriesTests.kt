package it.pagopa.checkout.authservice.repositories.redis

import it.pagopa.checkout.authservice.repositories.redis.bean.auth.*
import it.pagopa.checkout.authservice.repositories.redis.bean.oidc.*
import java.time.Duration
import kotlin.test.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

class WrappedRedisTemplateRepositoriesTests {

    @Test
    fun `should extract key correctly from domain object for AuthenticatedUserSessionRepository`() {
        val key = "key"
        val domainObject =
            AuthenticatedUserSession(
                sessionToken = SessionToken(key),
                userInfo =
                    UserInfo(
                        name = Name("name"),
                        surname = Name("surname"),
                        fiscalCode = UserFiscalCode("fiscalCode"),
                    ),
            )
        val extractedKey =
            AuthenticatedUserSessionRepository(redisTemplate = mock(), Duration.ofSeconds(1))
                .getKeyFromEntity(domainObject)
        assertEquals(key, extractedKey)
    }

    @Test
    fun `should extract key correctly from domain object for OIDCAuthStateDataRepository`() {
        val key = "key"
        val domainObject = OidcAuthStateData(state = OidcState(key), nonce = OidcNonce("nonce"))
        val extractedKey =
            OIDCAuthStateDataRepository(redisTemplate = mock(), Duration.ofSeconds(1))
                .getKeyFromEntity(domainObject)
        assertEquals(key, extractedKey)
    }

    @Test
    fun `should extract key correctly from domain object for OidcKeysRepository`() {
        val key = "key"
        val domainObject = OidcKey(kid = key, e = "exponent", n = "modulus")
        val extractedKey =
            OidcKeysRepository(redisTemplate = mock(), Duration.ofSeconds(1))
                .getKeyFromEntity(domainObject)
        assertEquals(key, extractedKey)
    }
}
