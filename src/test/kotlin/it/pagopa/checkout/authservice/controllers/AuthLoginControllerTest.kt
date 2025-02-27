package it.pagopa.checkout.authservice.controllers

import it.pagopa.checkout.authservice.exception.SessionValidationException
import it.pagopa.checkout.authservice.services.AuthenticationService
import it.pagopa.generated.checkout.authservice.v1.model.LoginResponseDto
import it.pagopa.generated.checkout.authservice.v1.model.ProblemJsonDto
import it.pagopa.generated.checkout.authservice.v1.model.UserInfoResponseDto
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
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
    fun `unimplemented endpoints should return 501 NOT_IMPLEMENTED`() {
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
}
