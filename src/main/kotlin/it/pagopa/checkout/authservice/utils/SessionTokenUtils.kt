package it.pagopa.checkout.authservice.utils

import it.pagopa.checkout.authservice.exception.SessionValidationException
import it.pagopa.checkout.authservice.repositories.redis.bean.auth.SessionToken
import java.security.SecureRandom
import java.util.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.stereotype.Component
import reactor.core.publisher.Mono

@Component
class SessionTokenUtils(
    @Value("\${session-token.lengthInBytes}") private val sessionTokenLengthInBytes: Int
) {

    private val secureRandom = SecureRandom()

    fun generateSessionToken(): SessionToken {
        val sessionToken = ByteArray(sessionTokenLengthInBytes)
        secureRandom.nextBytes(sessionToken)
        return SessionToken(Base64.getEncoder().encodeToString(sessionToken))
    }

    fun getBearerTokenFromRequestHeaders(request: ServerHttpRequest): Mono<String> {
        return Mono.justOrEmpty(request.headers.getFirst(HttpHeaders.AUTHORIZATION))
            .switchIfEmpty(
                Mono.error(SessionValidationException(message = "Missing Session Token"))
            )
            .map { header -> header.substring(7) } // TODO: remove magic numbers
    }
}
