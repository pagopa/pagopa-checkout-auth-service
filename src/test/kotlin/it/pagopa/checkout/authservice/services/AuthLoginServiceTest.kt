package it.pagopa.checkout.authservice.services

import it.pagopa.checkout.authservice.clients.oneidentity.OneIdentityClient
import it.pagopa.checkout.authservice.exception.OneIdentityClientException
import it.pagopa.generated.checkout.authservice.v1.model.LoginResponseDto
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.context.annotation.Import
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

@WebFluxTest(AuthLoginService::class)
@Import(OneIdentityClient::class)
class AuthLoginServiceTest {
    private val oneIdentityClient: OneIdentityClient = mock()
    private val authLoginService = AuthLoginService(oneIdentityClient)

    private val expectedUrl = "https://mock.example.com/client/login"
    private val rptId = "mock-rptid"

    @Test
    fun `login should return LoginResponseDto with redirect URL from OneIdentityClient`() {
        val expectedUrl = expectedUrl
        val rptId = rptId
        whenever(oneIdentityClient.buildLoginUrl()).thenReturn(Mono.just(expectedUrl))

        StepVerifier.create(authLoginService.login())
            .expectNextMatches { response -> response.urlRedirect == expectedUrl }
            .verifyComplete()
    }

    @Test
    fun `login should return Mono with properly constructed LoginResponseDto`() {
        val expectedUrl = expectedUrl
        val rptId = rptId
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
        val rptId = rptId
        val expectedError = RuntimeException("Failed to build URL")

        whenever(oneIdentityClient.buildLoginUrl()).thenReturn(Mono.error(expectedError))

        StepVerifier.create(authLoginService.login())
            .expectError(RuntimeException::class.java)
            .verify()
    }

    @Test
    fun `should throw OneIdentityClientException when client returns blank URL`() {
        val rptId = rptId
        whenever(oneIdentityClient.buildLoginUrl()).thenReturn(Mono.just(""))

        StepVerifier.create(authLoginService.login())
            .expectError(OneIdentityClientException::class.java)
            .verify()
    }
}
