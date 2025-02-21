package it.pagopa.checkout.authservice.controllers

import it.pagopa.checkout.authservice.services.AuthLoginService
import it.pagopa.generated.checkout.authservice.v1.model.LoginResponseDto
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.http.HttpStatus
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class AuthLoginControllerTest {
    private lateinit var authLoginController: AuthLoginController
    private lateinit var authLoginService: AuthLoginService

    @BeforeEach
    fun setup() {
        authLoginService = mock()
        authLoginController = AuthLoginController(authLoginService)
    }

    @Test
    fun `authLogin should return successful response when service returns login URL`() {
        val loginResponse = LoginResponseDto()
        loginResponse.urlRedirect = "https://mock.example.com/login?param=value"

        val xForwardedFor = "127.0.0.1"
        val xRptId = null

        whenever(authLoginService.login("N/A")).thenReturn(Mono.just(loginResponse))

        val result = authLoginController.authLogin(xForwardedFor, xRptId, null)

        StepVerifier.create(result)
            .expectNextMatches { response ->
                response.statusCode.is2xxSuccessful &&
                    response.body?.urlRedirect == "https://mock.example.com/login?param=value"
            }
            .verifyComplete()
    }

    @Test
    fun `authLogin should handle service errors with null rptid`() {
        // Setup
        val xForwardedFor = "127.0.0.1"
        val xRptId = null
        val expectedError = RuntimeException("Test error message")

        whenever(authLoginService.login("N/A")).thenReturn(Mono.error(expectedError))

        StepVerifier.create(authLoginController.authLogin(xForwardedFor, xRptId, null))
            .expectErrorMatches { error -> error.message == "Test error message" }
            .verify()
    }

    @Test
    fun `authLogin should handle service errors with provided rptid`() {
        val xForwardedFor = "127.0.0.1"
        val xRptId = "mock-rptid"
        val expectedError = RuntimeException("Test error message")

        whenever(authLoginService.login(xRptId)).thenReturn(Mono.error(expectedError))

        StepVerifier.create(authLoginController.authLogin(xForwardedFor, xRptId, null))
            .expectErrorMatches { error -> error.message == "Test error message" }
            .verify()
    }

    @Test
    fun `unimplemented endpoints should return 501 NOT_IMPLEMENTED`() {
        StepVerifier.create(authLoginController.authUsers(null))
            .expectNextMatches { response -> response.statusCode == HttpStatus.NOT_IMPLEMENTED }
            .verifyComplete()

        StepVerifier.create(authLoginController.authLogout(null))
            .expectNextMatches { response -> response.statusCode == HttpStatus.NOT_IMPLEMENTED }
            .verifyComplete()

        StepVerifier.create(authLoginController.authenticateWithAuthToken(null, null))
            .expectNextMatches { response -> response.statusCode == HttpStatus.NOT_IMPLEMENTED }
            .verifyComplete()

        StepVerifier.create(authLoginController.validateToken(null))
            .expectNextMatches { response -> response.statusCode == HttpStatus.NOT_IMPLEMENTED }
            .verifyComplete()
    }
}
