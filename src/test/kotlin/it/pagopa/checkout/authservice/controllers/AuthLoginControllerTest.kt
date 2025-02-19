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

        whenever(authLoginService.login()).thenReturn(Mono.just(loginResponse))

        val result = authLoginController.authLogin(null)

        StepVerifier.create(result)
            .expectNextMatches { response ->
                response.statusCode.is2xxSuccessful &&
                    response.body?.urlRedirect == "https://mock.example.com/login?param=value"
            }
            .verifyComplete()
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
