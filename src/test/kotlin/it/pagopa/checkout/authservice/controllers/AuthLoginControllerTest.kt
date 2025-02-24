package it.pagopa.checkout.authservice.controllers

import it.pagopa.checkout.authservice.services.AuthLoginService
import it.pagopa.generated.checkout.authservice.v1.model.LoginResponseDto
import org.junit.jupiter.api.Test
import org.mockito.kotlin.whenever
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

    @MockitoBean private lateinit var authLoginService: AuthLoginService

    @Test
    fun `authLogin should return successful response when service returns login URL`() {
        val loginResponse = LoginResponseDto()
        loginResponse.urlRedirect = "https://mock.example.com/login?param=value"

        whenever(authLoginService.login("N/A")).thenReturn(Mono.just(loginResponse))

        webClient
            .get()
            .uri("/auth/login")
            .header("X-Forwarded-For", "127.0.0.1")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .is2xxSuccessful
            .expectBody()
            .jsonPath("$.urlRedirect")
            .isEqualTo("https://mock.example.com/login?param=value")
    }

    @Test
    fun `authLogin should handle service errors with null rptid`() {
        whenever(authLoginService.login("N/A"))
            .thenReturn(Mono.error(RuntimeException("Test error message")))

        webClient
            .get()
            .uri("/auth/login")
            .header("X-Forwarded-For", "127.0.0.1")
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .is5xxServerError
    }

    @Test
    fun `authLogin should handle service errors with provided rptid`() {
        val xRptId = "mock-rptid"
        whenever(authLoginService.login(xRptId))
            .thenReturn(Mono.error(RuntimeException("Test error message")))

        webClient
            .get()
            .uri("/auth/login")
            .header("X-Forwarded-For", "127.0.0.1")
            .header("X-Rpt-Id", xRptId)
            .accept(MediaType.APPLICATION_JSON)
            .exchange()
            .expectStatus()
            .is5xxServerError
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
            .post()
            .uri("/auth/token")
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
