package it.pagopa.checkout.authservice.client.oneidentity

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component

@Component
class OneIdentityClient(
    @Value("\${oneidentity.base-url}") private val oneIdentityBaseUrl: String,
    @Value("\${oneidentity.redirect-uri}") private val redirectUri: String,
    @Value("\${openid.client-id}") private val clientId: String,
) {
    fun buildLoginUrl(): String {

        // Value opaque to the server, used by the client to track its session.
        // It will be returned as received.
        val state = UUID.randomUUID().toString()

        // Represents a cryptographically strong random string that is used to prevent
        // intercepted responses from being reused.
        val nonce = UUID.randomUUID().toString()

        val encodedUrl = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8.toString())

        return "$oneIdentityBaseUrl/login?" +
            listOf(
                    "response_type=code",
                    "scope=openid",
                    "client_id=$clientId",
                    "state=$state",
                    "nonce=$nonce",
                    "redirect_uri=$encodedUrl",
                )
                .joinToString("&")
    }
}
