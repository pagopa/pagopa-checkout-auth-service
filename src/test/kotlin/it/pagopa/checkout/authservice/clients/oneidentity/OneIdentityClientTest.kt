package it.pagopa.checkout.authservice.clients.oneidentity

import it.pagopa.checkout.authservice.exception.OneIdentityConfigurationException
import it.pagopa.generated.checkout.oneidentity.api.TokenServerApisApi
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.mockito.kotlin.mock
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
}
