package it.pagopa.checkout.authservice.utils

import com.fasterxml.jackson.databind.ObjectMapper
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Jws
import io.jsonwebtoken.Jwts
import it.pagopa.checkout.authservice.clients.oneidentity.OneIdentityClient
import it.pagopa.checkout.authservice.exception.OneIdentityConfigurationException
import it.pagopa.checkout.authservice.repositories.redis.OidcKeysRepository
import it.pagopa.checkout.authservice.repositories.redis.bean.oidc.OidcKey
import java.math.BigInteger
import java.nio.charset.StandardCharsets
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

    private val objectMapper = ObjectMapper()

    private val rsaKeyFactory = KeyFactory.getInstance("RSA")

    data class JwtKeyInfo(val kid: String?, val alg: String?)

    companion object {
        const val OI_JWT_USER_NAME_CLAIM_KEY = "name"
        const val OI_JWT_USER_FAMILY_NAME_CLAIM_KEY = "familyName"
        const val OI_JWT_USER_FISCAL_CODE_CLAIM_KEY = "fiscalNumber"
        const val OI_JWT_NONCE_CLAIM_KEY = "nonce"
        const val DEFAULT_JWT_KEY_ID = "default-kid"
    }

    fun validateAndParse(jwtToken: String): Mono<Jws<Claims>> =
        retrieveTokenKey(jwtToken).map {
            Jwts.parser().verifyWith(it).build().parse(jwtToken).accept(Jws.CLAIMS)
        }

    private fun retrieveTokenKey(jwtToken: String): Mono<PublicKey> =
        Mono.just(jwtToken)
            .map {
                val tokenHeader =
                    Base64.getDecoder()
                        .decode(jwtToken.substring(0, jwtToken.indexOf('.')))
                        .toString(StandardCharsets.UTF_8)

                val parsedTokenHeader = objectMapper.readTree(tokenHeader)
                val keyInfo =
                    JwtKeyInfo(
                        alg = parsedTokenHeader["alg"]?.asText(),
                        kid = parsedTokenHeader["kid"]?.asText(),
                    )
                logger.debug(
                    "Parsed token header: [{}], extracted key info: {}",
                    tokenHeader,
                    keyInfo,
                )
                keyInfo
            }
            .flatMap { keyInfo ->
                /*
                 *   kid is an optional jwt token parameter, if present
                 */
                val cachedKey =
                    Optional.ofNullable(keyInfo.kid)
                        .map { oidcKeysRepository.findById(it) }
                        .orElseGet {
                            val savedKeys = oidcKeysRepository.keysInKeyspace()
                            if (savedKeys.isNotEmpty()) {
                                oidcKeysRepository.findById(savedKeys.first())
                            } else {
                                null
                            }
                        }

                if (cachedKey == null) {
                    // cache miss
                    logger.info(
                        "Cache miss for kid: [{}], retrieving keys from One Identity",
                        keyInfo.kid,
                    )
                    oneIdentityClient.getKeys().flatMap { jwkResponse ->
                        if (jwkResponse.keys.size > 1 && keyInfo.kid == null) {
                            throw OneIdentityConfigurationException(
                                "Cannot determine which key to use: more keys returned and jwt token does not contain a kid header"
                            )
                        }
                        val key =
                            jwkResponse.keys.firstOrNull {
                                // if jwt token does not specify a kid just use the first returned
                                // one
                                if (keyInfo.kid != null) {
                                    it["kid"] == keyInfo.kid
                                } else {
                                    true
                                }
                            }
                        if (key == null) {
                            Mono.error(
                                OneIdentityConfigurationException(
                                    "Cannot find key with kid: [${keyInfo.kid}]"
                                )
                            )
                        } else {
                            val modulus = BigInteger(1, Base64.getUrlDecoder().decode(key["n"]))
                            val exponent = BigInteger(1, Base64.getUrlDecoder().decode(key["e"]))
                            val decodedPublicKey =
                                rsaKeyFactory.generatePublic(RSAPublicKeySpec(modulus, exponent))
                            oidcKeysRepository.save(
                                OidcKey(
                                    kid = key["kid"] ?: DEFAULT_JWT_KEY_ID,
                                    n = key["n"]!!,
                                    e = key["e"]!!,
                                )
                            )
                            Mono.just(decodedPublicKey)
                        }
                    }
                } else {
                    logger.info("Cache hit for key with kid: [{}]", cachedKey.kid)
                    Mono.just(
                        rsaKeyFactory.generatePublic(
                            RSAPublicKeySpec(
                                BigInteger(1, Base64.getUrlDecoder().decode(cachedKey.n)),
                                BigInteger(1, Base64.getUrlDecoder().decode(cachedKey.e)),
                            )
                        )
                    )
                }
            }
}
