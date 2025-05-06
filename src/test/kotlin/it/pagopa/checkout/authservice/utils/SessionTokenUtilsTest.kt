package it.pagopa.checkout.authservice.utils

import it.pagopa.checkout.authservice.v1.exception.SessionValidationException
import it.pagopa.checkout.authservice.v1.utils.SessionTokenUtils
import java.util.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import reactor.test.StepVerifier

class SessionTokenUtilsTest {

    private val tokenLength = 16
    private val sessionTokenUtils = SessionTokenUtils(tokenLength)

    @Test
    fun `Should generate random generated session token`() {
        val sessionToken1 = sessionTokenUtils.generateSessionToken()
        val sessionToken2 = sessionTokenUtils.generateSessionToken()
        assertTrue(sessionToken1.value != sessionToken2.value)
        assertEquals(tokenLength, Base64.getDecoder().decode(sessionToken1.value).size)
        assertEquals(tokenLength, Base64.getDecoder().decode(sessionToken2.value).size)
    }

    @Test
    fun `getSessionTokenFromRequest should extract token when valid bearer token is present`() {

        val token = "valid-token-123"
        val request =
            MockServerHttpRequest.get("/")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .build()

        val result = sessionTokenUtils.getSessionTokenFromRequest(request)

        StepVerifier.create(result).expectNext(token).verifyComplete()
    }

    @Test
    fun `getSessionTokenFromRequest should handle case-insensitive bearer prefix`() {

        val token = "valid-token-123"
        val request =
            MockServerHttpRequest.get("/")
                .header(HttpHeaders.AUTHORIZATION, "bearer $token")
                .build()

        val result = sessionTokenUtils.getSessionTokenFromRequest(request)

        StepVerifier.create(result).expectNext(token).verifyComplete()
    }

    @Test
    fun `getSessionTokenFromRequest should return error when authorization header is missing`() {

        val request = MockServerHttpRequest.get("/").build()

        val result = sessionTokenUtils.getSessionTokenFromRequest(request)

        StepVerifier.create(result)
            .expectErrorMatches { error ->
                error is SessionValidationException && error.message == "Missing or invalid token"
            }
            .verify()
    }

    @Test
    fun `getSessionTokenFromRequest should return error when authorization header is empty`() {

        val request = MockServerHttpRequest.get("/").header(HttpHeaders.AUTHORIZATION, "").build()

        val result = sessionTokenUtils.getSessionTokenFromRequest(request)

        StepVerifier.create(result)
            .expectErrorMatches { error ->
                error is SessionValidationException && error.message == "Missing or invalid token"
            }
            .verify()
    }
}
