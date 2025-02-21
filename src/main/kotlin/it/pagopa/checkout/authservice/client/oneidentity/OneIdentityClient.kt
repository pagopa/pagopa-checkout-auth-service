package it.pagopa.checkout.authservice.client.oneidentity

import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder

@Component
class OneIdentityClient(
    @Value("\${oneidentity.base-url}") private val oneIdentityBaseUrl: String,
    @Value("\${oneidentity.redirect-uri}") private val redirectUri: String,
    @Value("\${oneidentity.client-id}") private val clientId: String,
) {
    fun buildLoginUrl(): String {

        // Value opaque to the server, used by the client to track its session.
        // It will be returned as received.
        val state = UUID.randomUUID().toString()

        // Represents a cryptographically strong random string that is used to prevent
        // intercepted responses from being reused.
        val nonce = UUID.randomUUID().toString()

        val encodedUrl = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8.toString())

        return UriComponentsBuilder.fromUriString(oneIdentityBaseUrl)
            .path("/login")
            .queryParam("response_type", "code")
            .queryParam("scope", "openid")
            .queryParam("client_id", clientId)
            .queryParam("state", state)
            .queryParam("nonce", nonce)
            .queryParam("redirect_uri", encodedUrl)
            .build()
            .toUriString()
    }
}
