package it.pagopa.checkout.authservice.utils

import com.fasterxml.jackson.databind.ObjectMapper
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Header
import io.jsonwebtoken.Jwt
import io.jsonwebtoken.Jwts
import it.pagopa.checkout.authservice.clients.oneidentity.OneIdentityClient
import it.pagopa.checkout.authservice.exception.OneIdentityConfigurationException
import it.pagopa.checkout.authservice.repositories.redis.OidcKeysRepository
import it.pagopa.checkout.authservice.repositories.redis.bean.oidc.OidcKey
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.math.BigInteger
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.PublicKey
import java.security.spec.RSAPublicKeySpec
import java.util.*

@Component
class JwtUtils(
    val oidcKeysRepository: OidcKeysRepository,
    val oneIdentityClient: OneIdentityClient,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val objectMapper = ObjectMapper()

    private val rsaKeyFactory = KeyFactory.getInstance("RSA")

    data class JwtKeyInfo(val kid: String, val alg: String)

    companion object {
        const val OI_JWT_USER_NAME_CLAIM_KEY = "name"
        const val OI_JWT_USER_FAMILY_NAME_CLAIM_KEY = "familyName"
        const val OI_JWT_USER_FISCAL_CODE_CLAIM_KEY = "fiscalNumber"
    }


    fun validateAndParse(jwtToken: String): Mono<Jwt<Header, Claims>> =
        retrieveTokenKey(jwtToken)
            .map {
                Jwts.parser()
                    .verifyWith(it)
                    .build()
                    .parse(jwtToken)
                    .accept(Jwt.UNSECURED_CLAIMS)
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
                        alg = parsedTokenHeader["alg"].asText(),
                        kid = parsedTokenHeader["kid"].asText(),
                    )
                logger.debug(
                    "Parsed token header: [{}], extracted key info: {}",
                    tokenHeader,
                    keyInfo,
                )
                keyInfo
            }
            .flatMap { keyInfo ->
                val cachedKey = oidcKeysRepository.findById(keyInfo.kid)
                if (cachedKey == null) {
                    // cache miss
                    logger.info(
                        "Cache miss for kid: [{}], retrieving keys from One Identity",
                        keyInfo.kid,
                    )
                    oneIdentityClient
                        .getKeys()
                        .flatMap { jwkResponse ->
                            val key = jwkResponse
                                .keys
                                .map { objectMapper.readTree(it) }
                                .firstOrNull { it["kid"].asText() == keyInfo.kid }
                            if (key == null) {
                                Mono.error(OneIdentityConfigurationException("Cannot find key with kid: [${keyInfo.kid}]"))
                            } else {
                                val modulus = BigInteger(
                                    1,
                                    Base64.getDecoder().decode(key["n"].asText())
                                )
                                val exponent = BigInteger(
                                    1,
                                    Base64.getDecoder().decode(key["e"].asText())
                                )
                                val decodedPublicKey = rsaKeyFactory.generatePublic(RSAPublicKeySpec(modulus, exponent))
                                oidcKeysRepository.save(
                                    OidcKey(
                                        kid = key["kid"].asText(),
                                        n = key["n"].asText(),
                                        e = key["e"].asText(),
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
                                BigInteger(
                                    1,
                                    Base64.getDecoder().decode(cachedKey.n)
                                ),
                                BigInteger(
                                    1,
                                    Base64.getDecoder().decode(cachedKey.e)
                                )
                            )
                        )
                    )
                }
            }
}
