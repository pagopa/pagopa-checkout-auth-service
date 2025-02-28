package it.pagopa.checkout.authservice.controllers

import it.pagopa.checkout.authservice.exception.AuthFailedException
import it.pagopa.checkout.authservice.exception.OneIdentityServerException
import it.pagopa.checkout.authservice.repositories.redis.bean.auth.*
import it.pagopa.checkout.authservice.repositories.redis.bean.oidc.AuthCode
import it.pagopa.checkout.authservice.repositories.redis.bean.oidc.OidcState
import it.pagopa.checkout.authservice.services.AuthenticationService
import it.pagopa.generated.checkout.authservice.v1.model.AuthRequestDto
import it.pagopa.generated.checkout.authservice.v1.model.LoginResponseDto
import it.pagopa.generated.checkout.authservice.v1.model.ProblemJsonDto
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import reactor.core.publisher.Mono

@WebFluxTest(AuthLoginController::class)
class AuthLoginControllerTest {

    @Autowired private lateinit var webClient: WebTestClient

    @MockitoBean private lateinit var authenticationService: AuthenticationService

    @Test
    fun `authLogin should return successful response when service returns login URL`() {
        val loginResponse = LoginResponseDto()
        loginResponse.urlRedirect = "https://mock.example.com/login?param=value"

        whenever(authenticationService.login()).thenReturn(Mono.just(loginResponse))

        webClient
            .get()
            .uri("/auth/login")
            .header("127.0.0.1")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .is2xxSuccessful
            .expectBody()
            .jsonPath("$.urlRedirect")
            .isEqualTo("https://mock.example.com/login?param=value")
    }

    @Test
    fun `unimplemented endpoints should return 501 NOT_IMPLEMENTED`() {
        webClient
            .get()
            .uri("/auth/users")
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.NOT_IMPLEMENTED)
            .expectBody()
            .isEmpty()

        webClient
            .post()
            .uri("/auth/logout")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue("{}")
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.NOT_IMPLEMENTED)
            .expectBody()
            .isEmpty()

        webClient
            .get()
            .uri("/auth/validate")
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.NOT_IMPLEMENTED)
            .expectBody()
            .isEmpty()
    }

    @Test
    fun `should handle authentication with auth token successfully`() {
        // pre-conditions
        val authCode = "authCode"
        val state = "state"
        val sessionToken = "sessionToken"
        val authRequest = AuthRequestDto().authCode(authCode).state(state)
        val authenticatedUserSessionData =
            AuthenticatedUserSession(
                sessionToken = SessionToken(sessionToken),
                userInfo =
                    UserInfo(
                        name = Name("name"),
                        surname = Name("surname"),
                        fiscalCode = UserFiscalCode("userFiscalCode11"),
                    ),
            )
        given(authenticationService.retrieveAuthToken(any(), any()))
            .willReturn(Mono.just(authenticatedUserSessionData))
        // test
        webClient
            .post()
            .uri("/auth/token")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(authRequest)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody()
            .jsonPath("$.authToken")
            .isEqualTo(sessionToken)
        verify(authenticationService, times(1))
            .retrieveAuthToken(authCode = AuthCode(authCode), state = OidcState(state))
    }

    @Test
    fun `should return 401 error for AuthFailedException raised while performing authentication with auth token`() {
        // pre-conditions
        val authCode = "authCode"
        val state = "oidcState"
        val authRequest = AuthRequestDto().authCode(authCode).state(state)
        given(authenticationService.retrieveAuthToken(any(), any()))
            .willReturn(
                Mono.error(AuthFailedException(message = "error", state = OidcState(state)))
            )
        val expectedProblemJson =
            ProblemJsonDto()
                .status(401)
                .title("Unauthorized")
                .detail("Cannot perform authentication process for state: [oidcState]")
        // test
        webClient
            .post()
            .uri("/auth/token")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(authRequest)
            .exchange()
            .expectStatus()
            .isUnauthorized
            .expectBody(ProblemJsonDto::class.java)
            .isEqualTo(expectedProblemJson)
        verify(authenticationService, times(1))
            .retrieveAuthToken(authCode = AuthCode(authCode), state = OidcState(state))
    }

    @Test
    fun `should return 500 error for OneIdentityServerException raised while performing authentication with auth token`() {
        // pre-conditions
        val authCode = "authCode"
        val state = "oidcState"
        val authRequest = AuthRequestDto().authCode(authCode).state(state)
        given(authenticationService.retrieveAuthToken(any(), any()))
            .willReturn(
                Mono.error(OneIdentityServerException(message = "error", state = OidcState(state)))
            )
        val expectedProblemJson =
            ProblemJsonDto()
                .status(500)
                .title("Error communicating with One identity")
                .detail("Cannot perform authentication process for state: [oidcState]")
        // test
        webClient
            .post()
            .uri("/auth/token")
            .contentType(MediaType.APPLICATION_JSON)
            .bodyValue(authRequest)
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)
            .expectBody(ProblemJsonDto::class.java)
            .isEqualTo(expectedProblemJson)
        verify(authenticationService, times(1))
            .retrieveAuthToken(authCode = AuthCode(authCode), state = OidcState(state))
    }
}
