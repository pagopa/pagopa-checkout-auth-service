package it.pagopa.checkout.authservice.services

import io.jsonwebtoken.Jwts
import it.pagopa.checkout.authservice.clients.oneidentity.LoginData
import it.pagopa.checkout.authservice.clients.oneidentity.OneIdentityClient
import it.pagopa.checkout.authservice.exception.AuthFailedException
import it.pagopa.checkout.authservice.exception.SessionValidationException
import it.pagopa.checkout.authservice.repositories.redis.AuthenticatedUserSessionRepository
import it.pagopa.checkout.authservice.repositories.redis.OIDCAuthStateDataRepository
import it.pagopa.checkout.authservice.repositories.redis.bean.auth.*
import it.pagopa.checkout.authservice.repositories.redis.bean.oidc.AuthCode
import it.pagopa.checkout.authservice.repositories.redis.bean.oidc.OidcAuthStateData
import it.pagopa.checkout.authservice.repositories.redis.bean.oidc.OidcNonce
import it.pagopa.checkout.authservice.repositories.redis.bean.oidc.OidcState
import it.pagopa.checkout.authservice.utils.JwtUtils
import it.pagopa.checkout.authservice.utils.SessionTokenUtils
import it.pagopa.generated.checkout.authservice.v1.model.LoginResponseDto
import it.pagopa.generated.checkout.authservice.v1.model.UserInfoResponseDto
import it.pagopa.generated.checkout.oneidentity.model.TokenDataDto
import java.util.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class AuthenticationServiceTest {
    private val oneIdentityClient: OneIdentityClient = mock()
    private val oidcAuthStateDataRepository: OIDCAuthStateDataRepository = mock()
    private val authenticatedUserSessionRepository: AuthenticatedUserSessionRepository = mock()
    private val jwtUtils: JwtUtils = mock()
    private val sessionTokenUtils: SessionTokenUtils = mock()
    private val authenticationService =
        AuthenticationService(
            oneIdentityClient = oneIdentityClient,
            oidcAuthStateDataRepository = oidcAuthStateDataRepository,
            authenticatedUserSessionRepository = authenticatedUserSessionRepository,
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

        given(oidcAuthStateDataRepository.save(any())).willAnswer { invocation ->
            Mono.just(invocation.getArgument<AuthenticatedUserSession>(0))
        }

        StepVerifier.create(authenticationService.login())
            .expectNextMatches { response -> response.urlRedirect == expectedUrl }
            .verifyComplete()
    }

    @Test
    fun `login should return Mono with properly constructed LoginResponseDto`() {
        val expectedUrl = expectedUrl
        whenever(oneIdentityClient.buildLoginUrl()).thenReturn(Mono.just(loginData))

        given(oidcAuthStateDataRepository.save(any())).willAnswer { invocation ->
            Mono.just(invocation.getArgument<AuthenticatedUserSession>(0))
        }

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
            Mono.just(
                AuthenticatedUserSession(
                    userInfo =
                        UserInfo(
                            fiscalCode = UserFiscalCode("RSSMRA80A01H501U"),
                            name = Name("Mario"),
                            surname = Name("Rossi"),
                        ),
                    sessionToken = SessionToken(sessionToken),
                )
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
        whenever(authenticatedUserSessionRepository.findById(sessionToken)).thenReturn(Mono.empty())

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
            Mono.just(
                AuthenticatedUserSession(
                    userInfo =
                        UserInfo(
                            fiscalCode = UserFiscalCode("RSSMRA80A01H501U"),
                            name = Name("Mario"),
                            surname = Name("Rossi"),
                        ),
                    sessionToken = SessionToken(sessionToken),
                )
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

    @Test
    fun `should retrieve auth token successfully retrieving info from OneIdentity`() {
        // pre-requisites
        val oidcState = OidcState("state")
        val oidcNonce = OidcNonce("nonce")
        val authCode = AuthCode("authCode")
        val oidcCacheAuthState = Mono.just(OidcAuthStateData(state = oidcState, nonce = oidcNonce))
        val userName = "name"
        val userFamilyName = "familyName"
        val userFiscalCode = "userFiscalCode"
        val idToken = "idToken"
        val sessionToken = SessionToken("sessionToken")
        val tokenDataDtoResponse = TokenDataDto().idToken(idToken)
        val jwtResponseClaims = Jwts.claims()
        jwtResponseClaims[JwtUtils.OI_JWT_NONCE_CLAIM_KEY] = oidcNonce.value
        jwtResponseClaims[JwtUtils.OI_JWT_USER_NAME_CLAIM_KEY] = userName
        jwtResponseClaims[JwtUtils.OI_JWT_USER_FAMILY_NAME_CLAIM_KEY] = userFamilyName
        jwtResponseClaims[JwtUtils.OI_JWT_USER_FISCAL_CODE_CLAIM_KEY] = userFiscalCode
        given(oidcAuthStateDataRepository.findById(any())).willReturn(oidcCacheAuthState)
        given(oneIdentityClient.retrieveOidcToken(any(), any()))
            .willReturn(Mono.just(tokenDataDtoResponse))
        given(jwtUtils.validateAndParse(any())).willReturn(Mono.just(jwtResponseClaims))
        given(sessionTokenUtils.generateSessionToken()).willReturn(sessionToken)
        given(authenticatedUserSessionRepository.save(any())).willReturn(Mono.empty())
        given(oidcAuthStateDataRepository.deleteById(any())).willReturn(Mono.just(true))
        // test
        val expectedAuthenticatedUserSession =
            AuthenticatedUserSession(
                sessionToken = sessionToken,
                userInfo =
                    UserInfo(
                        name = Name(userName),
                        surname = Name(userFamilyName),
                        fiscalCode = UserFiscalCode(userFiscalCode),
                    ),
            )
        StepVerifier.create(
                authenticationService.retrieveAuthToken(authCode = authCode, state = oidcState)
            )
            .expectNext(expectedAuthenticatedUserSession)
            .verifyComplete()
        verify(oidcAuthStateDataRepository, times(1)).findById(oidcState.value)
        verify(authenticatedUserSessionRepository, times(0)).findById(any())
        verify(oneIdentityClient, times(1))
            .retrieveOidcToken(authCode = authCode, state = oidcState)
        verify(sessionTokenUtils, times(1)).generateSessionToken()
        verify(authenticatedUserSessionRepository, times(1)).save(expectedAuthenticatedUserSession)
        verify(oidcAuthStateDataRepository, times(1)).deleteById(oidcState.value)
    }

    @Test
    fun `should retrieve auth token successfully for first call and throw error on second call`() {

        val oidcState = OidcState("state")
        val oidcNonce = OidcNonce("nonce")
        val authCode = AuthCode("authCode")
        val oidcCachedAuthState = Mono.just(OidcAuthStateData(state = oidcState, nonce = oidcNonce))

        val userName = "name"
        val userFamilyName = "familyName"
        val userFiscalCode = "userFiscalCode"
        val idToken = "idToken"
        val sessionToken = SessionToken("sessionToken")

        val tokenDataDtoResponse = TokenDataDto().idToken(idToken)

        val jwtResponseClaims =
            Jwts.claims().apply {
                this[JwtUtils.OI_JWT_NONCE_CLAIM_KEY] = oidcNonce.value
                this[JwtUtils.OI_JWT_USER_NAME_CLAIM_KEY] = userName
                this[JwtUtils.OI_JWT_USER_FAMILY_NAME_CLAIM_KEY] = userFamilyName
                this[JwtUtils.OI_JWT_USER_FISCAL_CODE_CLAIM_KEY] = userFiscalCode
            }

        given(oidcAuthStateDataRepository.findById(oidcState.value))
            .willReturn(oidcCachedAuthState)
            .willReturn(Mono.empty())

        given(oneIdentityClient.retrieveOidcToken(authCode, oidcState))
            .willReturn(Mono.just(tokenDataDtoResponse))

        given(jwtUtils.validateAndParse(tokenDataDtoResponse.idToken))
            .willReturn(Mono.just(jwtResponseClaims))

        given(sessionTokenUtils.generateSessionToken()).willReturn(sessionToken)
        given(oidcAuthStateDataRepository.deleteById(oidcState.value)).willReturn(Mono.just(true))
        given(authenticatedUserSessionRepository.save(any())).willAnswer { invocation ->
            Mono.just(invocation.getArgument<AuthenticatedUserSession>(0))
        }
        val expectedAuthenticatedUserSession =
            AuthenticatedUserSession(
                sessionToken = sessionToken,
                userInfo =
                    UserInfo(
                        name = Name(userName),
                        surname = Name(userFamilyName),
                        fiscalCode = UserFiscalCode(userFiscalCode),
                    ),
            )

        StepVerifier.create(authenticationService.retrieveAuthToken(authCode, oidcState))
            .expectNext(expectedAuthenticatedUserSession)
            .verifyComplete()

        StepVerifier.create(authenticationService.retrieveAuthToken(authCode, oidcState))
            .expectErrorMatches { ex ->
                ex is AuthFailedException &&
                    ex.message?.contains("Cannot retrieve OIDC session") == true
            }
            .verify()

        verify(oidcAuthStateDataRepository, times(2)).findById(oidcState.value)
        verify(oidcAuthStateDataRepository, times(1)).deleteById(oidcState.value)
        verify(oneIdentityClient, times(1)).retrieveOidcToken(authCode, oidcState)
    }

    @Test
    fun `should throw error for cached nonce and jwt token mismatch`() {
        // pre-requisites
        val oidcState = OidcState("state")
        val cacheNonce = OidcNonce("cacheNonce")
        val jwtNonce = cacheNonce.value + "_jwtNonce"
        val authCode = AuthCode("authCode")
        val oidcCacheAuthState = OidcAuthStateData(state = oidcState, nonce = cacheNonce)
        val userName = "name"
        val userFamilyName = "familyName"
        val userFiscalCode = "userFiscalCode"
        val idToken = "idToken"
        val sessionToken = SessionToken("sessionToken")
        val tokenDataDtoResponse = TokenDataDto().idToken(idToken)
        val jwtResponseClaims = Jwts.claims()
        jwtResponseClaims[JwtUtils.OI_JWT_NONCE_CLAIM_KEY] = jwtNonce
        jwtResponseClaims[JwtUtils.OI_JWT_USER_NAME_CLAIM_KEY] = userName
        jwtResponseClaims[JwtUtils.OI_JWT_USER_FAMILY_NAME_CLAIM_KEY] = userFamilyName
        jwtResponseClaims[JwtUtils.OI_JWT_USER_FISCAL_CODE_CLAIM_KEY] = userFiscalCode
        given(oidcAuthStateDataRepository.findById(any())).willReturn(Mono.just(oidcCacheAuthState))
        given(oneIdentityClient.retrieveOidcToken(any(), any()))
            .willReturn(Mono.just(tokenDataDtoResponse))
        given(jwtUtils.validateAndParse(any())).willReturn(Mono.just(jwtResponseClaims))
        given(sessionTokenUtils.generateSessionToken()).willReturn(sessionToken)
        given(authenticatedUserSessionRepository.save(any())).willReturn(Mono.empty())
        given(oidcAuthStateDataRepository.deleteById(any())).willReturn(Mono.just(true))
        // test
        val expectedAuthenticatedUserSession =
            AuthenticatedUserSession(
                sessionToken = sessionToken,
                userInfo =
                    UserInfo(
                        name = Name(userName),
                        surname = Name(userFamilyName),
                        fiscalCode = UserFiscalCode(userFiscalCode),
                    ),
            )
        StepVerifier.create(
                authenticationService.retrieveAuthToken(authCode = authCode, state = oidcState)
            )
            .consumeErrorWith {
                assertTrue(it is AuthFailedException)
                assertEquals(
                    "Authentication process error for state: [state] -> Nonce mismatch! id token value: [cacheNonce_jwtNonce], cached value: [cacheNonce]",
                    it.message,
                )
            }
            .verify()
        verify(oidcAuthStateDataRepository, times(1)).findById(oidcState.value)
        verify(authenticatedUserSessionRepository, times(0)).findById(any())
        verify(oneIdentityClient, times(1))
            .retrieveOidcToken(authCode = authCode, state = oidcState)
        verify(sessionTokenUtils, times(0)).generateSessionToken()
        verify(authenticatedUserSessionRepository, times(0)).save(any())
        verify(oidcAuthStateDataRepository, times(0)).deleteById(any())
    }

    @Test
    fun `should not throw validation exception on cache hit`() {
        // pre-requisites
        val request = MockServerHttpRequest.get("/").build()
        val bearerToken = "bearerToken"
        val authenticatedUserSession =
            Mono.just(
                AuthenticatedUserSession(
                    userInfo =
                        UserInfo(
                            fiscalCode = UserFiscalCode("RSSMRA80A01H501U"),
                            name = Name("Mario"),
                            surname = Name("Rossi"),
                        ),
                    sessionToken = SessionToken(bearerToken),
                )
            )
        // test mock
        given { sessionTokenUtils.getSessionTokenFromRequest(request) }
            .willReturn(Mono.just(bearerToken))
        given { authenticatedUserSessionRepository.findById(bearerToken) }
            .willReturn(authenticatedUserSession)
        // test
        StepVerifier.create(authenticationService.validateAuthToken(request))
            .expectNext(Unit)
            .verifyComplete()
        verify(sessionTokenUtils, times(1)).getSessionTokenFromRequest(request)
        verify(authenticatedUserSessionRepository, times(1)).findById(bearerToken)
    }

    @Test
    fun `should throw validation exception on cache miss`() {
        // pre-requisites
        val request = MockServerHttpRequest.get("/").build()
        val bearerToken = "bearerToken"
        // test mock
        given { sessionTokenUtils.getSessionTokenFromRequest(request) }
            .willReturn(Mono.just(bearerToken))
        given { authenticatedUserSessionRepository.findById(bearerToken) }.willReturn(Mono.empty())
        // test
        StepVerifier.create(authenticationService.validateAuthToken(request))
            .expectError(SessionValidationException::class.java)
            .verify()
        verify(sessionTokenUtils, times(1)).getSessionTokenFromRequest(request)
        verify(authenticatedUserSessionRepository, times(1)).findById(bearerToken)
    }

    @Test
    fun `should complete successfully on delete existing session on logout`() {
        // pre-requisites
        val request = MockServerHttpRequest.get("/").build()
        val bearerToken = "bearerToken"
        // test mock
        given { sessionTokenUtils.getSessionTokenFromRequest(request) }
            .willReturn(Mono.just(bearerToken))
        given { authenticatedUserSessionRepository.deleteById(bearerToken) }
            .willReturn(Mono.just(true))
        // test
        StepVerifier.create(authenticationService.logout(request)).expectNext(Unit).verifyComplete()
        verify(sessionTokenUtils, times(1)).getSessionTokenFromRequest(request)
        verify(authenticatedUserSessionRepository, times(1)).deleteById(bearerToken)
    }

    @Test
    fun `should complete successfully on delete not existing session on logout`() {
        // pre-requisites
        val request = MockServerHttpRequest.get("/").build()
        val bearerToken = "bearerToken"
        // test mock
        given { sessionTokenUtils.getSessionTokenFromRequest(request) }
            .willReturn(Mono.just(bearerToken))
        given { authenticatedUserSessionRepository.deleteById(bearerToken) }
            .willReturn(Mono.just(false))
        // test
        StepVerifier.create(authenticationService.logout(request)).expectNext(Unit).verifyComplete()
        verify(sessionTokenUtils, times(1)).getSessionTokenFromRequest(request)
        verify(authenticatedUserSessionRepository, times(1)).deleteById(bearerToken)
    }

    @Test
    fun `should throw validation exception on missing or malformed request authentication header`() {
        // pre-requisites
        val request = MockServerHttpRequest.get("/").build()
        val bearerToken = "bearerToken"
        // test mock
        given { sessionTokenUtils.getSessionTokenFromRequest(request) }
            .willReturn(
                Mono.error(SessionValidationException(message = "Missing or invalid token"))
            )
        // test
        StepVerifier.create(authenticationService.validateAuthToken(request))
            .expectError(SessionValidationException::class.java)
            .verify()
        verify(sessionTokenUtils, times(1)).getSessionTokenFromRequest(request)
        verify(authenticatedUserSessionRepository, times(0)).findById(bearerToken)
    }
}
