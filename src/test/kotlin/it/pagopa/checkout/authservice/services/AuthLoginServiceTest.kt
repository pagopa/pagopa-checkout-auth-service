package it.pagopa.checkout.authservice.services

import it.pagopa.checkout.authservice.client.oneidentity.OneIdentityClient
import it.pagopa.generated.checkout.authservice.v1.model.LoginResponseDto
import kotlin.test.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import reactor.test.StepVerifier

class AuthLoginServiceTest {
    private lateinit var authLoginService: AuthLoginService
    private lateinit var oneIdentityClient: OneIdentityClient

    @BeforeEach
    fun setup() {
        oneIdentityClient = mock()
        authLoginService = AuthLoginService(oneIdentityClient)
    }

    @Test
    fun `login should return LoginResponseDto with redirect URL from OneIdentityClient`() {
        val expectedUrl = "https://mock.example.com/login?param=value"
        val rptId = "mock-rptid"
        whenever(oneIdentityClient.buildLoginUrl()).thenReturn(expectedUrl)

        val result = authLoginService.login(rptId)

        StepVerifier.create(result)
            .assertNext { response -> assertEquals(expectedUrl, response.urlRedirect) }
            .verifyComplete()
    }

    @Test
    fun `login should return Mono with properly constructed LoginResponseDto`() {
        val expectedUrl = "https://mock.example.com/login?param=value"
        val rptId = "mock-rptid"
        whenever(oneIdentityClient.buildLoginUrl()).thenReturn(expectedUrl)

        val result = authLoginService.login(rptId)

        StepVerifier.create(result)
            .assertNext { response ->
                assertEquals(LoginResponseDto::class.java, response::class.java)
                assertEquals(expectedUrl, response.urlRedirect)
            }
            .verifyComplete()
    }

    @Test
    fun `login should handle OneIdentityClient errors properly`() {
        val rptId = "mock-rptid"
        val expectedError = RuntimeException("Failed to build URL")

        whenever(oneIdentityClient.buildLoginUrl()).thenThrow(expectedError)

        StepVerifier.create(authLoginService.login(rptId))
            .expectErrorMatches { error ->
                error == expectedError && error.message == "Failed to build URL"
            }
            .verify()
    }
}
