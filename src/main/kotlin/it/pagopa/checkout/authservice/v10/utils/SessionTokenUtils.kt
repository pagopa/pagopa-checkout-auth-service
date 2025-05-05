package it.pagopa.checkout.authservice.v10.utils

import it.pagopa.checkout.authservice.v10.exception.SessionValidationException
import it.pagopa.checkout.authservice.v10.repositories.redis.bean.auth.SessionToken
import java.security.SecureRandom
import java.util.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component("SessionTokenUtilsv10")
class SessionTokenUtils(
    @Value("\${session-token.lengthInBytes}") private val sessionTokenLengthInBytes: Int
) {

    private val secureRandom = SecureRandom()

    private val BEARER_PREFIX = "Bearer "

    fun generateSessionToken(): SessionToken {
        val sessionToken = ByteArray(sessionTokenLengthInBytes)
        secureRandom.nextBytes(sessionToken)
        return SessionToken(Base64.getEncoder().encodeToString(sessionToken))
    }

    fun getSessionTokenFromRequest(request: ServerHttpRequest): Mono<String> {
        return Mono.justOrEmpty(
                request.headers
                    .getFirst(HttpHeaders.AUTHORIZATION)
                    ?.takeIf { it.startsWith(BEARER_PREFIX, ignoreCase = true) }
                    ?.substring(BEARER_PREFIX.length)
            )
            .switchIfEmpty(
                Mono.error(SessionValidationException(message = "Missing or invalid token"))
            )
    }
}
