package it.pagopa.checkout.authservice.client.oneidentity

import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import java.util.*
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class OneIdentityClientTest {
    private lateinit var oneIdentityClient: OneIdentityClient

    private val baseUrl = "https://mock.example.com"
    private val clientId = "mock-client-id"
    private val redirectUri = "https://mock.example.com/client/login"

    @BeforeEach
    fun setup() {
        oneIdentityClient = OneIdentityClient(baseUrl, clientId, redirectUri)
    }

    @Test
    fun `buildLoginUrl should return URL with all required parameters`() {

        val result = oneIdentityClient.buildLoginUrl()

        assertTrue(result.startsWith("$baseUrl/login"))

        // convert parameters to key-value map by splitting them
        val params =
            result.substringAfter("?").split("&").map { it.split("=") }.associate { it[0] to it[1] }

        assertEquals("code", params["response_type"])
        assertEquals("openid", params["scope"])
        assertEquals(clientId, params["client_id"])

        assertTrue(isValidUUID(params["state"]))
        assertTrue(isValidUUID(params["nonce"]))

        val decodedRedirectUri =
            URLDecoder.decode(params["redirect_uri"], StandardCharsets.UTF_8.toString())
        assertEquals(redirectUri, decodedRedirectUri)
    }

    @Test
    fun `buildLoginUrl should generate different state and nonce for each call`() {

        val firstResult = oneIdentityClient.buildLoginUrl()
        val secondResult = oneIdentityClient.buildLoginUrl()

        val firstParams =
            firstResult
                .substringAfter("?")
                .split("&")
                .map { it.split("=") }
                .associate { it[0] to it[1] }

        val secondParams =
            secondResult
                .substringAfter("?")
                .split("&")
                .map { it.split("=") }
                .associate { it[0] to it[1] }

        assertTrue(firstParams["state"] != secondParams["state"])
        assertTrue(firstParams["nonce"] != secondParams["nonce"])
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
}
