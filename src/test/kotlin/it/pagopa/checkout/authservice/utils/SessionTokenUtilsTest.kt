package it.pagopa.checkout.authservice.utils

import it.pagopa.checkout.authservice.exception.SessionValidationException
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import reactor.test.StepVerifier

class SessionTokenUtilsTest {

    private val sessionTokenUtils = SessionTokenUtils(32) // Using a sample length

    @Test
    fun `getSessionTokenFromRequest should extract token when valid bearer token is present`() {
        // Given
        val token = "valid-token-123"
        val request =
            MockServerHttpRequest.get("/")
                .header(HttpHeaders.AUTHORIZATION, "Bearer $token")
                .build()

        // When
        val result = sessionTokenUtils.getSessionTokenFromRequest(request)

        // Then
        StepVerifier.create(result).expectNext(token).verifyComplete()
    }

    @Test
    fun `getSessionTokenFromRequest should handle case-insensitive bearer prefix`() {
        // Given
        val token = "valid-token-123"
        val request =
            MockServerHttpRequest.get("/")
                .header(HttpHeaders.AUTHORIZATION, "bearer $token")
                .build()

        // When
        val result = sessionTokenUtils.getSessionTokenFromRequest(request)

        // Then
        StepVerifier.create(result).expectNext(token).verifyComplete()
    }

    @Test
    fun `getSessionTokenFromRequest should return error when authorization header is missing`() {
        // Given
        val request = MockServerHttpRequest.get("/").build()

        // When
        val result = sessionTokenUtils.getSessionTokenFromRequest(request)

        // Then
        StepVerifier.create(result)
            .expectErrorMatches { error ->
                error is SessionValidationException && error.message == "Missing or invalid token"
            }
            .verify()
    }

    @Test
    fun `getSessionTokenFromRequest should return error when authorization header is empty`() {
        // Given
        val request = MockServerHttpRequest.get("/").header(HttpHeaders.AUTHORIZATION, "").build()

        // When
        val result = sessionTokenUtils.getSessionTokenFromRequest(request)

        // Then
        StepVerifier.create(result)
            .expectErrorMatches { error ->
                error is SessionValidationException && error.message == "Missing or invalid token"
            }
            .verify()
    }
}
