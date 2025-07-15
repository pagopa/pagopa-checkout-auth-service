package it.pagopa.checkout.authservice.utils

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import it.pagopa.checkout.authservice.clients.oneidentity.OneIdentityClient
import it.pagopa.checkout.authservice.exception.OneIdentityConfigurationException
import it.pagopa.checkout.authservice.exception.OneIdentityServerException
import it.pagopa.checkout.authservice.repositories.redis.OidcKeysRepository
import it.pagopa.checkout.authservice.repositories.redis.bean.oidc.OidcKey
import java.math.BigInteger
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.RSAPublicKeySpec
import java.util.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono

@Component
class JwtUtils(
    private val oidcKeysRepository: OidcKeysRepository,
    private val oneIdentityClient: OneIdentityClient,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val rsaKeyFactory = KeyFactory.getInstance("RSA")

    companion object {
        const val OI_JWT_USER_NAME_CLAIM_KEY = "name"
        const val OI_JWT_USER_FAMILY_NAME_CLAIM_KEY = "familyName"
        const val OI_JWT_USER_FISCAL_CODE_CLAIM_KEY = "fiscalNumber"
        const val OI_JWT_NONCE_CLAIM_KEY = "nonce"
    }

    fun validateAndParse(jwtToken: String): Mono<Claims> {
        return retrieveTokenKeys().flatMap { publicKeys ->
            val jwtParser =
                publicKeys.map {
                    Mono.just(it).map { publicKey ->
                        logger.debug("Validating jwt with publicKey: {}", publicKey)
                        Jwts.parserBuilder().setSigningKey(publicKey).build().parse(jwtToken).body
                            as Claims
                    }
                }
            Mono.firstWithValue(jwtParser).onErrorResume { exception ->
                logger.error(
                    "Error performing signature validation for received JWT idToken",
                    exception,
                )
                Mono.fromCallable { oidcKeysRepository.deleteAll() }
                    .doOnNext {
                        logger.warn(
                            "No cached key were valid to validate JWT token, cleared cached keys: [{}]",
                            it,
                        )
                    }
                    .then(Mono.error(exception))
            }
        }
    }

    private fun retrieveTokenKeys(): Mono<List<PublicKey>> =
        oidcKeysRepository
            .getAllValues()
            .collectList()
            .flatMap { cachedKeys ->
                if (cachedKeys.isEmpty()) {
                    logger.info("Cache miss for JWT token keys, recovering from One Identity")
                    oneIdentityClient.getKeys().flatMap { jwkResponse ->
                        val rsaKeys = jwkResponse.keys.filter { it["kty"] == "RSA" }

                        if (rsaKeys.isEmpty()) {
                            return@flatMap Mono.error<List<OidcKey>>(
                                OneIdentityConfigurationException(
                                    "Cannot find any key with type: [RSA] to be used for verify token"
                                )
                            )
                        }

                        Flux.fromIterable(rsaKeys)
                            .flatMap { keyMap ->
                                val kid = keyMap["kid"]
                                val n = keyMap["n"]
                                val e = keyMap["e"]

                                if (kid.isNullOrBlank() || n.isNullOrBlank() || e.isNullOrBlank()) {
                                    return@flatMap Mono.error<OidcKey>(
                                        OneIdentityServerException(
                                            message =
                                                "Invalid public key detected, null kid, n or e fields. Decoded key: $keyMap",
                                            state = null,
                                        )
                                    )
                                }

                                val oidcKey = OidcKey(kid = kid, n = n, e = e)
                                oidcKeysRepository.save(oidcKey).thenReturn(oidcKey)
                            }
                            .collectList()
                    }
                } else {
                    logger.info("Cache hit for keys")
                    Mono.just(cachedKeys)
                }
            }
            .map { keys ->
                keys.map { key ->
                    val modulus = BigInteger(1, Base64.getUrlDecoder().decode(key.n))
                    val exponent = BigInteger(1, Base64.getUrlDecoder().decode(key.e))
                    rsaKeyFactory.generatePublic(RSAPublicKeySpec(modulus, exponent))
                }
            }
}
