package it.pagopa.checkout.authservice.services

import it.pagopa.checkout.authservice.clients.oneidentity.OneIdentityClient
import it.pagopa.generated.checkout.authservice.v1.model.LoginResponseDto
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class AuthLoginServiceTest {
    private val oneIdentityClient: OneIdentityClient = mock()
    private val authLoginService = AuthLoginService(oneIdentityClient)

    private val expectedUrl = "https://mock.example.com/client/login"

    @Test
    fun `login should return LoginResponseDto with redirect URL from OneIdentityClient`() {
        val expectedUrl = expectedUrl
        whenever(oneIdentityClient.buildLoginUrl()).thenReturn(Mono.just(expectedUrl))

        StepVerifier.create(authLoginService.login())
            .expectNextMatches { response -> response.urlRedirect == expectedUrl }
            .verifyComplete()
    }

    @Test
    fun `login should return Mono with properly constructed LoginResponseDto`() {
        val expectedUrl = expectedUrl
        whenever(oneIdentityClient.buildLoginUrl()).thenReturn(Mono.just(expectedUrl))

        StepVerifier.create(authLoginService.login())
            .expectNextMatches { response ->
                response.javaClass == LoginResponseDto::class.java &&
                    response.urlRedirect == expectedUrl
            }
            .verifyComplete()
    }

    @Test
    fun `login should handle OneIdentityClient errors properly`() {
        val expectedError = RuntimeException("Failed to build URL")

        whenever(oneIdentityClient.buildLoginUrl()).thenReturn(Mono.error(expectedError))

        StepVerifier.create(authLoginService.login())
            .expectError(RuntimeException::class.java)
            .verify()
    }
}
