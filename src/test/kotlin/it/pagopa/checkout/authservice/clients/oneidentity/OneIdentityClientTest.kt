package it.pagopa.checkout.authservice.clients.oneidentity

import it.pagopa.checkout.authservice.exception.OneIdentityConfigurationException
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest
import reactor.test.StepVerifier

@WebFluxTest(OneIdentityClient::class)
class OneIdentityClientTest {

    private val baseUrl = "https://mock.example.com"
    private val redirectUri = "https://mock.example.com/client/login"
    private val clientId = "oneidentity-client-id"

    private val oneIdentityClient =
        OneIdentityClient(
            oneIdentityBaseUrl = baseUrl,
            redirectUri = redirectUri,
            clientId = clientId,
        )

    @Test
    fun `buildLoginUrl should return URL with all required parameters`() {
        StepVerifier.create(oneIdentityClient.buildLoginUrl())
            .consumeNextWith { result ->
                assertTrue(result.startsWith("$baseUrl/login"))

                // convert parameters to key-value map by splitting them
                val params =
                    result
                        .substringAfter("?")
                        .split("&")
                        .map { it.split("=") }
                        .associate { it[0] to it[1] }

                assertEquals("code", params["response_type"])
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
                    firstResult
                        .substringAfter("?")
                        .split("&")
                        .map { it.split("=") }
                        .associate { it[0] to it[1] }

                StepVerifier.create(secondMono)
                    .consumeNextWith { secondResult ->
                        // convert parameters to key-value map by splitting them
                        val secondParams =
                            secondResult
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
            OneIdentityClient(oneIdentityBaseUrl = baseUrl, redirectUri = "", clientId = clientId)

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
