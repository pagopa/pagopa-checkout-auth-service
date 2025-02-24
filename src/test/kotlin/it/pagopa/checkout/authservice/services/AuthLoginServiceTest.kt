package it.pagopa.checkout.authservice.services

import it.pagopa.checkout.authservice.client.oneidentity.OneIdentityClient
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

    @Test
    fun `login should return LoginResponseDto with redirect URL from OneIdentityClient`() {
        val expectedUrl = "https://mock.example.com/login?param=value"
        val rptId = "mock-rptid"
        whenever(oneIdentityClient.buildLoginUrl()).thenReturn(Mono.just(expectedUrl))

        StepVerifier.create(authLoginService.login(rptId))
            .expectNextMatches { response -> response.urlRedirect == expectedUrl }
            .verifyComplete()
    }

    @Test
    fun `login should return Mono with properly constructed LoginResponseDto`() {
        val expectedUrl = "https://mock.example.com/login?param=value"
        val rptId = "mock-rptid"
        whenever(oneIdentityClient.buildLoginUrl()).thenReturn(Mono.just(expectedUrl))

        StepVerifier.create(authLoginService.login(rptId))
            .expectNextMatches { response ->
                response.javaClass == LoginResponseDto::class.java &&
                    response.urlRedirect == expectedUrl
            }
            .verifyComplete()
    }

    @Test
    fun `login should handle OneIdentityClient errors properly`() {
        val rptId = "mock-rptid"
        val expectedError = RuntimeException("Failed to build URL")

        whenever(oneIdentityClient.buildLoginUrl()).thenReturn(Mono.error(expectedError))

        StepVerifier.create(authLoginService.login(rptId))
            .expectError(RuntimeException::class.java)
            .verify()
    }
}
