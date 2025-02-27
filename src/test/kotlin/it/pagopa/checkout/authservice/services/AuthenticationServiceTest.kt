package it.pagopa.checkout.authservice.services

import it.pagopa.checkout.authservice.clients.oneidentity.LoginData
import it.pagopa.checkout.authservice.clients.oneidentity.OneIdentityClient
import it.pagopa.checkout.authservice.exception.SessionValidationException
import it.pagopa.checkout.authservice.repositories.redis.AuthSessionTokenRepository
import it.pagopa.checkout.authservice.repositories.redis.AuthenticatedUserSessionRepository
import it.pagopa.checkout.authservice.repositories.redis.OIDCAuthStateDataRepository
import it.pagopa.checkout.authservice.repositories.redis.bean.auth.*
import it.pagopa.checkout.authservice.repositories.redis.bean.oidc.OidcNonce
import it.pagopa.checkout.authservice.repositories.redis.bean.oidc.OidcState
import it.pagopa.checkout.authservice.utils.JwtUtils
import it.pagopa.checkout.authservice.utils.SessionTokenUtils
import it.pagopa.generated.checkout.authservice.v1.model.LoginResponseDto
import it.pagopa.generated.checkout.authservice.v1.model.UserInfoResponseDto
import java.util.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class AuthenticationServiceTest {
    private val oneIdentityClient: OneIdentityClient = mock()
    private val oidcAuthStateDataRepository: OIDCAuthStateDataRepository = mock()
    private val authenticatedUserSessionRepository: AuthenticatedUserSessionRepository = mock()
    private val authSessionTokenRepository: AuthSessionTokenRepository = mock()
    private val jwtUtils: JwtUtils = mock()
    private val sessionTokenUtils: SessionTokenUtils = mock()
    private val authenticationService =
        AuthenticationService(
            oneIdentityClient = oneIdentityClient,
            oidcAuthStateDataRepository = oidcAuthStateDataRepository,
            authenticatedUserSessionRepository = authenticatedUserSessionRepository,
            authSessionTokenRepository = authSessionTokenRepository,
            jwtUtils = jwtUtils,
            sessionTokenUtils = sessionTokenUtils,
        )

    private val expectedUrl = "https://mock.example.com/client/login"

    private val loginData =
        LoginData(
            loginRedirectUri = expectedUrl,
            state = OidcState(UUID.randomUUID()),
            nonce = OidcNonce(UUID.randomUUID()),
        )

    @Test
    fun `login should return LoginResponseDto with redirect URL from OneIdentityClient`() {
        val expectedUrl = expectedUrl
        whenever(oneIdentityClient.buildLoginUrl()).thenReturn(Mono.just(loginData))

        StepVerifier.create(authenticationService.login())
            .expectNextMatches { response -> response.urlRedirect == expectedUrl }
            .verifyComplete()
    }

    @Test
    fun `login should return Mono with properly constructed LoginResponseDto`() {
        val expectedUrl = expectedUrl
        whenever(oneIdentityClient.buildLoginUrl()).thenReturn(Mono.just(loginData))

        StepVerifier.create(authenticationService.login())
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

        StepVerifier.create(authenticationService.login())
            .expectError(RuntimeException::class.java)
            .verify()
    }

    @Test
    fun `getUserInfo should return user information when valid session token is present`() {

        val request = MockServerHttpRequest.get("/").build()
        val sessionToken = "valid-session-token"
        val authenticatedUserSession =
            AuthenticatedUserSession(
                userInfo =
                    UserInfo(
                        fiscalCode = UserFiscalCode("RSSMRA80A01H501U"),
                        name = Name("Mario"),
                        surname = Name("Rossi"),
                    ),
                sessionToken = SessionToken(sessionToken),
            )

        whenever(sessionTokenUtils.getSessionTokenFromRequest(request))
            .thenReturn(Mono.just(sessionToken))
        whenever(authenticatedUserSessionRepository.findById(sessionToken))
            .thenReturn(authenticatedUserSession)

        val result = authenticationService.getUserInfo(request)

        StepVerifier.create(result)
            .expectNextMatches { response ->
                response.userId == "RSSMRA80A01H501U" &&
                    response.name == "Mario" &&
                    response.familyName == "Rossi"
            }
            .verifyComplete()
    }

    @Test
    fun `getUserInfo should return error when session token is invalid`() {

        val request = MockServerHttpRequest.get("/").build()
        val sessionToken = "invalid-session-token"

        whenever(sessionTokenUtils.getSessionTokenFromRequest(request))
            .thenReturn(Mono.just(sessionToken))
        whenever(authenticatedUserSessionRepository.findById(sessionToken)).thenReturn(null)

        val result = authenticationService.getUserInfo(request)

        StepVerifier.create(result)
            .expectErrorMatches { error ->
                error is SessionValidationException &&
                    error.message == "Invalid or missing session token"
            }
            .verify()
    }

    @Test
    fun `getUserInfo should propagate error when session token extraction fails`() {

        val request = MockServerHttpRequest.get("/").build()
        val expectedError = SessionValidationException("Failed to extract token")

        whenever(sessionTokenUtils.getSessionTokenFromRequest(request))
            .thenReturn(Mono.error(expectedError))

        val result = authenticationService.getUserInfo(request)

        StepVerifier.create(result).expectError(SessionValidationException::class.java).verify()
    }

    @Test
    fun `getUserInfo should return properly constructed UserInfoResponseDto`() {

        val request = MockServerHttpRequest.get("/").build()
        val sessionToken = "valid-session-token"
        val authenticatedUserSession =
            AuthenticatedUserSession(
                userInfo =
                    UserInfo(
                        fiscalCode = UserFiscalCode("RSSMRA80A01H501U"),
                        name = Name("Mario"),
                        surname = Name("Rossi"),
                    ),
                sessionToken = SessionToken(sessionToken),
            )

        whenever(sessionTokenUtils.getSessionTokenFromRequest(request))
            .thenReturn(Mono.just(sessionToken))
        whenever(authenticatedUserSessionRepository.findById(sessionToken))
            .thenReturn(authenticatedUserSession)

        val result = authenticationService.getUserInfo(request)

        StepVerifier.create(result)
            .expectNextMatches { response ->
                response.javaClass == UserInfoResponseDto::class.java &&
                    response.userId == "RSSMRA80A01H501U" &&
                    response.name == "Mario" &&
                    response.familyName == "Rossi"
            }
            .verifyComplete()
    }
}
