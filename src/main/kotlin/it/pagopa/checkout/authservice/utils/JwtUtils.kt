package it.pagopa.checkout.authservice.utils

import com.fasterxml.jackson.databind.ObjectMapper
import io.jsonwebtoken.Claims
import io.jsonwebtoken.Header
import io.jsonwebtoken.Jwt
import io.jsonwebtoken.Jwts
import it.pagopa.checkout.authservice.clients.oneidentity.OneIdentityClient
import it.pagopa.checkout.authservice.repositories.redis.OidcKeysRepository
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono
import java.nio.charset.StandardCharsets
import java.security.PublicKey
import java.util.*

@Component
class JwtUtils(
    val oidcKeysRepository: OidcKeysRepository,
    val oneIdentityClient: OneIdentityClient,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    private val objectMapper = ObjectMapper()

    data class JwtKeyInfo(val kid: String, val alg: String)


    fun validateAndParse(jwtToken: String): Mono<Jwt<Header, Claims>> =
        Mono.just(
            Jwts.parser()
                .requireIssuer("https://issuer.example.com")
                .verifyWith(null as PublicKey) // TODO retrieve public key
                .build()
                .parse(jwtToken)
                .accept(Jwt.UNSECURED_CLAIMS)
        )

    private fun retrieveTokenKey(jwtToken: String): Mono<Any> =
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
            .flatMap {
                val cachedKey = oidcKeysRepository.findById(it.kid)
                if (cachedKey == null) {
                    // cache miss
                    logger.info(
                        "Cache miss for kid: [{}], retrieving keys from One Identity",
                        it.kid,
                    )
                    oneIdentityClient
                        .getKeys()
                        .map { jwkResponse ->
                            jwkResponse
                                .keys
                                .forEach()
                        }
                }
                Mono.empty<String>()
            }


}
