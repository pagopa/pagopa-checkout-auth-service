package it.pagopa.checkout.authservice.clients.oneidentity

import it.pagopa.checkout.authservice.exception.AuthFailedException
import it.pagopa.checkout.authservice.exception.OneIdentityBadGatewayException
import it.pagopa.checkout.authservice.exception.OneIdentityConfigurationException
import it.pagopa.checkout.authservice.exception.OneIdentityServerException
import it.pagopa.checkout.authservice.repositories.redis.bean.oidc.AuthCode
import it.pagopa.checkout.authservice.repositories.redis.bean.oidc.OidcState
import it.pagopa.generated.checkout.oneidentity.api.TokenServerApisApi
import it.pagopa.generated.checkout.oneidentity.model.GetJwkSet200ResponseDto
import it.pagopa.generated.checkout.oneidentity.model.TokenDataDto
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.stream.Stream
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import org.mockito.kotlin.*
import org.springframework.web.reactive.function.client.WebClientResponseException
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class OneIdentityClientTest {

    private val baseUrl = "https://mock.example.com"
    private val redirectUri = "https://mock.example.com/client/login"
    private val clientId = "one-identity-client-id"
    private val clientSecret = "one-identity-client-secret"
    private val tokenServerApisApi: TokenServerApisApi = mock()

    private val oneIdentityClient =
        OneIdentityClient(
            oneIdentityBaseUrl = baseUrl,
            redirectUri = redirectUri,
            clientId = clientId,
            clientSecret = clientSecret,
            oneIdentityWebClient = tokenServerApisApi,
        )

    @Test
    fun `buildLoginUrl should return URL with all required parameters`() {
        StepVerifier.create(oneIdentityClient.buildLoginUrl())
            .consumeNextWith { result ->
                val loginUrl = result.loginRedirectUri.toString()
                assertTrue(loginUrl.startsWith("$baseUrl/login"))
                assertNotNull(result.nonce)
                assertNotNull(result.state)

                // convert parameters to key-value map by splitting them
                val params =
                    loginUrl
                        .substringAfter("?")
                        .split("&")
                        .map { it.split("=") }
                        .associate { it[0] to it[1] }

                assertEquals("CODE", params["response_type"])
                assertEquals("openid", params["scope"])
                assertEquals(clientId, params["client_id"])

                assertTrue(isValidUUID(params["state"]))
                assertTrue(isValidUUID(params["nonce"]))

                val decodedRedirectUri =
                    URLDecoder.decode(params["redirect_uri"], StandardCharsets.UTF_8.toString())
                assertEquals(redirectUri, decodedRedirectUri)
            }
            .verifyComplete()
    }

    @Test
    fun `buildLoginUrl should generate different state and nonce for each call`() {
        val firstMono = oneIdentityClient.buildLoginUrl()
        val secondMono = oneIdentityClient.buildLoginUrl()

        StepVerifier.create(firstMono)
            .consumeNextWith { firstResult ->
                // convert parameters to key-value map by splitting them
                val firstParams =
                    firstResult.loginRedirectUri
                        .toString()
                        .substringAfter("?")
                        .split("&")
                        .map { it.split("=") }
                        .associate { it[0] to it[1] }

                StepVerifier.create(secondMono)
                    .consumeNextWith { secondResult ->
                        // convert parameters to key-value map by splitting them
                        val secondParams =
                            secondResult.loginRedirectUri
                                .toString()
                                .substringAfter("?")
                                .split("&")
                                .map { it.split("=") }
                                .associate { it[0] to it[1] }

                        assertTrue(firstParams["state"] != secondParams["state"])
                        assertTrue(firstParams["nonce"] != secondParams["nonce"])
                    }
                    .verifyComplete()
            }
            .verifyComplete()
    }

    private fun isValidUUID(uuid: String?): Boolean {
        if (uuid == null) return false
        return try {
            UUID.fromString(uuid)
            true
        } catch (e: IllegalArgumentException) {
            false
        }
    }

    @Test
    fun `buildLoginUrl should throw OneIdentityClientException when configuration parameters are missing`() {
        val clientWithBlankBaseUrl =
            OneIdentityClient(
                oneIdentityBaseUrl = "",
                redirectUri = redirectUri,
                clientId = clientId,
                oneIdentityWebClient = tokenServerApisApi,
                clientSecret = clientSecret,
            )

        val exceptionBlankBaseUrl =
            assertThrows<OneIdentityConfigurationException> {
                clientWithBlankBaseUrl.buildLoginUrl().block()
            }
        assertEquals(
            "Required OneIdentity configuration parameters are missing",
            exceptionBlankBaseUrl.message,
        )

        val clientWithBlankRedirectUri =
            OneIdentityClient(
                oneIdentityBaseUrl = baseUrl,
                redirectUri = "",
                clientId = clientId,
                oneIdentityWebClient = tokenServerApisApi,
                clientSecret = clientSecret,
            )

        val exceptionBlankRedirectUri =
            assertThrows<OneIdentityConfigurationException> {
                clientWithBlankRedirectUri.buildLoginUrl().block()
            }
        assertEquals(
            "Required OneIdentity configuration parameters are missing",
            exceptionBlankRedirectUri.message,
        )

        val clientWithBlankClientId =
            OneIdentityClient(
                oneIdentityBaseUrl = baseUrl,
                redirectUri = redirectUri,
                clientId = "",
                oneIdentityWebClient = tokenServerApisApi,
                clientSecret = clientSecret,
            )

        val exceptionBlankClientId =
            assertThrows<OneIdentityConfigurationException> {
                clientWithBlankClientId.buildLoginUrl().block()
            }
        assertEquals(
            "Required OneIdentity configuration parameters are missing",
            exceptionBlankClientId.message,
        )
    }

    @Test
    fun `should retrieve OIDC token successfully performing POST auth-token`() {
        // pre-conditions
        val authCode = AuthCode("authCode")
        val state = OidcState("state")
        val tokenServerResponse = TokenDataDto()
        val expectedAuthorizationField =
            Base64.getEncoder()
                .encodeToString("$clientId:$clientSecret".toByteArray(StandardCharsets.UTF_8))
        given(tokenServerApisApi.createRequestToken(any(), any(), any(), any()))
            .willReturn(Mono.just(tokenServerResponse))
        // test
        StepVerifier.create(oneIdentityClient.retrieveOidcToken(authCode = authCode, state = state))
            .expectNext(tokenServerResponse)
            .verifyComplete()
        // assertions
        verify(tokenServerApisApi, times(1))
            .createRequestToken(
                expectedAuthorizationField,
                redirectUri,
                authCode.value,
                "AUTHORIZATION_CODE",
            )
    }

    @Test
    fun `should fail to retrieve OIDC token when POST auth-token with duplicated code and return 502`() {
        // pre-conditions
        val authCode = AuthCode("authCode")
        val state = OidcState("state")
        val tokenServerResponse = TokenDataDto()
        val expectedAuthorizationField =
            Base64.getEncoder()
                .encodeToString("$clientId:$clientSecret".toByteArray(StandardCharsets.UTF_8))
        given(tokenServerApisApi.createRequestToken(any(), any(), any(), any()))
            .willReturn(Mono.just(tokenServerResponse))
            .willThrow(
                WebClientResponseException(
                    500,
                    "Internal Server Error",
                    org.springframework.http.HttpHeaders(),
                    ByteArray(0),
                    null,
                )
            )

        // test
        StepVerifier.create(oneIdentityClient.retrieveOidcToken(authCode = authCode, state = state))
            .expectNext(tokenServerResponse)
            .verifyComplete()

        StepVerifier.create(oneIdentityClient.retrieveOidcToken(authCode = authCode, state = state))
            .expectError(OneIdentityBadGatewayException::class.java)
            .verify()

        // assertions
        verify(tokenServerApisApi, times(2))
            .createRequestToken(
                expectedAuthorizationField,
                redirectUri,
                authCode.value,
                "AUTHORIZATION_CODE",
            )
    }

    @Test
    fun `should handle exception thrown by client while performing POST auth-token`() {
        // pre-conditions
        val authCode = AuthCode("authCode")
        val state = OidcState("state")
        val expectedAuthorizationField =
            Base64.getEncoder()
                .encodeToString("$clientId:$clientSecret".toByteArray(StandardCharsets.UTF_8))
        given(tokenServerApisApi.createRequestToken(any(), any(), any(), any()))
            .willThrow(RuntimeException("Some error"))
        // test
        StepVerifier.create(oneIdentityClient.retrieveOidcToken(authCode = authCode, state = state))
            .expectError(OneIdentityServerException::class.java)
            .verify()
        // assertions
        verify(tokenServerApisApi, times(1))
            .createRequestToken(
                expectedAuthorizationField,
                redirectUri,
                authCode.value,
                "AUTHORIZATION_CODE",
            )
    }

    @ParameterizedTest
    @MethodSource("post auth token error response mapping method source")
    fun `should map error responses to proper exception performing POST auth-token`(
        runtimeException: Exception,
        expectedRemappedException: Class<Exception>,
    ) {
        // pre-conditions
        val authCode = AuthCode("authCode")
        val state = OidcState("state")
        val expectedAuthorizationField =
            Base64.getEncoder()
                .encodeToString("$clientId:$clientSecret".toByteArray(StandardCharsets.UTF_8))
        given(tokenServerApisApi.createRequestToken(any(), any(), any(), any()))
            .willReturn(Mono.error(runtimeException))
        // test
        StepVerifier.create(oneIdentityClient.retrieveOidcToken(authCode = authCode, state = state))
            .expectError(expectedRemappedException)
            .verify()
        // assertions
        verify(tokenServerApisApi, times(1))
            .createRequestToken(
                expectedAuthorizationField,
                redirectUri,
                authCode.value,
                "AUTHORIZATION_CODE",
            )
    }

    @Test
    fun `should retrieve JWT keys successfully`() {
        // pre-condition
        val response = GetJwkSet200ResponseDto()
        given(tokenServerApisApi.jwkSet).willReturn(Mono.just(response))
        // test
        StepVerifier.create(oneIdentityClient.getKeys()).expectNext(response).verifyComplete()
        // assertions
        verify(tokenServerApisApi, times(1)).jwkSet
    }

    @Test
    fun `should handle exception thrown by webclient`() {
        // pre-condition
        given(tokenServerApisApi.jwkSet).willThrow(RuntimeException("some error"))
        // test
        StepVerifier.create(oneIdentityClient.getKeys())
            .expectError(OneIdentityConfigurationException::class.java)
            .verify()
        // assertions
        verify(tokenServerApisApi, times(1)).jwkSet
    }

    @Test
    fun `should handle error communicating with OI for retrieve keys`() {
        // pre-condition
        given(tokenServerApisApi.jwkSet).willReturn(Mono.error(RuntimeException("some error")))
        // test
        StepVerifier.create(oneIdentityClient.getKeys())
            .expectError(OneIdentityConfigurationException::class.java)
            .verify()
        // assertions
        verify(tokenServerApisApi, times(1)).jwkSet
    }

    companion object {
        @JvmStatic
        fun `post auth token error response mapping method source`(): Stream<Arguments> =
            Stream.of(
                Arguments.of(
                    WebClientResponseException("", 400, "", null, null, null),
                    OneIdentityBadGatewayException::class.java,
                ),
                Arguments.of(
                    WebClientResponseException("", 401, "", null, null, null),
                    AuthFailedException::class.java,
                ),
                Arguments.of(
                    WebClientResponseException("", 403, "", null, null, null),
                    AuthFailedException::class.java,
                ),
                Arguments.of(
                    WebClientResponseException("", 500, "", null, null, null),
                    OneIdentityBadGatewayException::class.java,
                ),
                Arguments.of(RuntimeException("test"), OneIdentityServerException::class.java),
            )
    }
}
