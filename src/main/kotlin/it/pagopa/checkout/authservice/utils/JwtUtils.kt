package it.pagopa.checkout.authservice.utils

import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jwts
import it.pagopa.checkout.authservice.clients.oneidentity.OneIdentityClient
import it.pagopa.checkout.authservice.exception.OneIdentityConfigurationException
import it.pagopa.checkout.authservice.repositories.redis.OidcKeysRepository
import it.pagopa.checkout.authservice.repositories.redis.bean.oidc.OidcKey
import java.math.BigInteger
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.RSAPublicKeySpec
import java.util.*
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
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
                    Mono.just(
                        Jwts.parserBuilder().setSigningKey(it).build().parse(jwtToken).body
                            as Claims
                    )
                }
            Mono.firstWithValue(jwtParser).onErrorResume {
                logger.error(
                    "Cannot validate jwt token with any of the input keys, clearing key cache",
                    it,
                )
                oidcKeysRepository.keysInKeyspace().forEach { kid ->
                    oidcKeysRepository.delete(kid)
                }
                Mono.error(it)
            }
        }
    }

    private fun retrieveTokenKeys(): Mono<List<PublicKey>> =
        Mono.just(1)
            .flatMap {
                /*
                 *   kid is an optional jwt token parameter, if present
                 */
                val cachedKeys = oidcKeysRepository.getAllValues()
                if (cachedKeys.isEmpty()) {
                    // cache miss
                    logger.info("Cache miss for JWT token keys, recovering from One Identity")
                    oneIdentityClient.getKeys().flatMap { jwkResponse ->
                        val decodedKeys =
                            jwkResponse.keys
                                .filter { it["kty"] == "RSA" } // filter for RSA keys only
                                .map {
                                    val oidcKey =
                                        OidcKey(kid = it["kid"]!!, n = it["n"]!!, e = it["e"]!!)
                                    // and save each key into cache with its kid as id
                                    oidcKeysRepository.save(oidcKey)
                                    oidcKey
                                }

                        if (decodedKeys.isEmpty()) {
                            Mono.error(
                                OneIdentityConfigurationException(
                                    "Cannot find any key with type: [RSA] to be used for verify token"
                                )
                            )
                        } else {
                            Mono.just(decodedKeys)
                        }
                    }
                } else {
                    logger.info("Cache hit for keys")
                    Mono.just(cachedKeys)
                }
            }
            .map { cachedKeys ->
                cachedKeys.map { key ->
                    val modulus = BigInteger(1, Base64.getUrlDecoder().decode(key.n))
                    val exponent = BigInteger(1, Base64.getUrlDecoder().decode(key.e))
                    rsaKeyFactory.generatePublic(RSAPublicKeySpec(modulus, exponent))
                }
            }
}
