package it.pagopa.checkout.authservice.services

import it.pagopa.checkout.authservice.clients.oneidentity.LoginData
import it.pagopa.checkout.authservice.clients.oneidentity.OneIdentityClient
import it.pagopa.checkout.authservice.repositories.redis.AuthSessionTokenRepository
import it.pagopa.checkout.authservice.repositories.redis.AuthenticatedUserSessionRepository
import it.pagopa.checkout.authservice.repositories.redis.OIDCAuthStateDataRepository
import it.pagopa.checkout.authservice.repositories.redis.bean.oidc.OidcAuthStateData
import it.pagopa.checkout.authservice.repositories.redis.bean.oidc.OidcNonce
import it.pagopa.checkout.authservice.repositories.redis.bean.oidc.OidcState
import it.pagopa.checkout.authservice.utils.JwtUtils
import it.pagopa.checkout.authservice.utils.SessionTokenUtils
import it.pagopa.generated.checkout.authservice.v1.model.LoginResponseDto
import it.pagopa.generated.checkout.oneidentity.model.TokenDataDto
import java.util.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.given
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
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
            state = OidcState(UUID.randomUUID().toString()),
            nonce = OidcNonce(UUID.randomUUID().toString()),
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
    fun `should retrieve auth token successfully retrieving info from OneIdentity (cache miss)`() {
        // pre-requisites
        val oidcState = OidcState("state")
        val oidcNonce = OidcNonce("nonce")
        val oidcCacheAuthState = OidcAuthStateData(state = oidcState, nonce = oidcNonce)

        val idToken = "idToken"
        val tokenDataDtoResponse = TokenDataDto().idToken(idToken)
        given(oidcAuthStateDataRepository.findById(any())).willReturn(oidcCacheAuthState)
        given(authSessionTokenRepository.findById(any())).willReturn(null)
        given(oneIdentityClient.retrieveOidcToken(any(), any()))
            .willReturn(Mono.just(tokenDataDtoResponse))
        // given(jwtUtils.validateAndParse(any())).willReturn(Mono.just(Jwts.claims().build()))
    }
}
