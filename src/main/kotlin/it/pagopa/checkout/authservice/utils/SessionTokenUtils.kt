package it.pagopa.checkout.authservice.utils

import it.pagopa.checkout.authservice.repositories.redis.bean.auth.SessionToken
import java.security.SecureRandom
import java.util.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

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
}
