package it.pagopa.checkout.authservice.controllers

import it.pagopa.checkout.authservice.exception.AuthFailedException
import it.pagopa.checkout.authservice.exception.OneIdentityServerException
import it.pagopa.checkout.authservice.exception.SessionValidationException
import it.pagopa.checkout.authservice.repositories.redis.bean.auth.*
import it.pagopa.checkout.authservice.repositories.redis.bean.oidc.AuthCode
import it.pagopa.checkout.authservice.repositories.redis.bean.oidc.OidcState
import it.pagopa.checkout.authservice.services.AuthenticationService
import it.pagopa.generated.checkout.authservice.v1.model.AuthRequestDto
import it.pagopa.generated.checkout.authservice.v1.model.LoginResponseDto
import it.pagopa.generated.checkout.authservice.v1.model.ProblemJsonDto
import it.pagopa.generated.checkout.authservice.v1.model.UserInfoResponseDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.whenever
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.test.context.bean.override.mockito.MockitoBean
import org.springframework.test.web.reactive.server.WebTestClient
import org.springframework.test.web.reactive.server.expectBody
import org.springframework.web.server.ServerWebInputException
import reactor.core.publisher.Mono
import reactor.kotlin.core.publisher.toMono

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
    fun `authUsers should return Unauthorized when session token is invalid`() {
        given { authenticationService.getUserInfo(any()) }
            .willReturn(
                Mono.error(SessionValidationException(message = "Invalid or missing session token"))
            )

        webClient
            .get()
            .uri("/auth/users")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isUnauthorized
            .expectBody<ProblemJsonDto>()
            .consumeWith { response ->
                with(response.responseBody!!) {
                    assertEquals(401, status)
                    assertEquals("Unauthorized", title)
                    assertEquals(
                        "Session validation failed: [Invalid or missing session token]",
                        detail,
                    )
                }
            }
    }

    @Test
    fun `authUsers should return user information when service returns valid user data`() {
        val userInfo =
            UserInfoResponseDto().apply {
                userId = "MRSI12L230DF476M"
                name = "Mario"
                familyName = "Rossi"
            }

        given { authenticationService.getUserInfo(any()) }.willReturn(userInfo.toMono())

        webClient
            .get()
            .uri("/auth/users")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .isOk
            .expectBody<UserInfoResponseDto>()
            .consumeWith { assertEquals(userInfo, it.responseBody) }
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
    fun `should return 502 error for OneIdentityServerException raised while performing authentication with auth token`() {
        // pre-conditions
        val authCode = "authCode"
        val state = "oidcState"
        val authRequest = AuthRequestDto().authCode(authCode).state(state)
        given(authenticationService.retrieveAuthToken(any(), any()))
            .willReturn(
                Mono.error(
                    OneIdentityServerException(
                        message = "error",
                        state = OidcState(state),
                        status = HttpStatus.BAD_GATEWAY,
                    )
                )
            )
        val expectedProblemJson =
            ProblemJsonDto()
                .status(502)
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
            .isEqualTo(HttpStatus.BAD_GATEWAY)
            .expectBody(ProblemJsonDto::class.java)
            .isEqualTo(expectedProblemJson)
        verify(authenticationService, times(1))
            .retrieveAuthToken(authCode = AuthCode(authCode), state = OidcState(state))
    }

    @Test
    fun `should return 204 in case of successful logout`() {
        // pre-conditions
        given(authenticationService.logout(any())).willReturn(Mono.just(Unit))
        // test
        webClient.post().uri("/auth/logout").exchange().expectStatus().isNoContent

        verify(authenticationService, times(1)).logout(any())
    }

    @Test
    fun `should return 400 on bad request`() {
        // pre-conditions
        given(authenticationService.logout(any()))
            .willReturn(Mono.error(ServerWebInputException("Test exception")))
        // test
        webClient.post().uri("/auth/logout").exchange().expectStatus().isBadRequest

        verify(authenticationService, times(1)).logout(any())
    }

    @Test
    fun `should return 401 on invalid or missing token`() {
        // pre-conditions
        given(authenticationService.logout(any()))
            .willReturn(Mono.error(SessionValidationException("Test exception")))
        // test
        webClient.post().uri("/auth/logout").exchange().expectStatus().isUnauthorized

        verify(authenticationService, times(1)).logout(any())
    }

    @Test
    fun `should return 500 on unexpected error`() {
        // pre-conditions
        given(authenticationService.logout(any()))
            .willReturn(Mono.error(Exception("Test exception")))
        // test
        webClient
            .post()
            .uri("/auth/logout")
            .exchange()
            .expectStatus()
            .isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR)

        verify(authenticationService, times(1)).logout(any())
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
    fun `should return 200 in case of successful validation with auth token`() {
        // pre-conditions
        given(authenticationService.validateAuthToken(any())).willReturn(Mono.just(Unit))
        // test
        webClient.get().uri("/auth/validate").exchange().expectStatus().isOk

        verify(authenticationService, times(1)).validateAuthToken(any())
    }

    @Test
    fun `should return 401 error for SessionValidationException raised while performing validation with auth token`() {
        // pre-conditions
        given(authenticationService.validateAuthToken(any()))
            .willReturn(Mono.error(SessionValidationException(message = "Invalid session token")))
        val expectedProblemJson =
            ProblemJsonDto()
                .status(401)
                .title("Unauthorized")
                .detail("Session validation failed: [Invalid session token]")
        // test
        webClient
            .get()
            .uri("/auth/validate")
            .exchange()
            .expectStatus()
            .isUnauthorized
            .expectBody(ProblemJsonDto::class.java)
            .isEqualTo(expectedProblemJson)

        verify(authenticationService, times(1)).validateAuthToken(any())
    }

    @Test
    fun `should return 500 error for Exception raised while performing validation with auth token`() {
        // pre-conditions
        given(authenticationService.validateAuthToken(any())).willReturn(Mono.error(Exception()))
        val expectedProblemJson =
            ProblemJsonDto()
                .status(500)
                .title("Internal Server Error")
                .detail("An unexpected error occurred processing the request")
        // test
        webClient
            .get()
            .uri("/auth/validate")
            .exchange()
            .expectStatus()
            .is5xxServerError
            .expectBody(ProblemJsonDto::class.java)
            .isEqualTo(expectedProblemJson)

        verify(authenticationService, times(1)).validateAuthToken(any())
    }
}
