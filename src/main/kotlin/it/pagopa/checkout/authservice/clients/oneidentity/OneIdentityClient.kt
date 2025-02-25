package it.pagopa.checkout.authservice.clients.oneidentity

import it.pagopa.checkout.authservice.exception.OneIdentityConfigurationException
import it.pagopa.checkout.authservice.repositories.redis.bean.oidc.OidcNonce
import it.pagopa.checkout.authservice.repositories.redis.bean.oidc.OidcState
import java.util.*
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono

@Component
class OneIdentityClient(
    @Value("\${oneidentity.base-url}") private val oneIdentityBaseUrl: String,
    @Value("\${oneidentity.redirect-uri}") private val redirectUri: String,
    @Value("\${oneidentity.client-id}") private val clientId: String,
) {
    fun buildLoginUrl(): Mono<LoginData> {
        if (oneIdentityBaseUrl.isBlank() || redirectUri.isBlank() || clientId.isBlank()) {
            return Mono.error(
                OneIdentityConfigurationException(
                    "Required OneIdentity configuration parameters are missing"
                )
            )
        }

        return Mono.just(oneIdentityBaseUrl)
            .map {
                // Value opaque to the server, used by the client to track its session.
                // It will be returned as received.
                val state = UUID.randomUUID()
                // Represents a cryptographically strong random string that is used to prevent
                // intercepted responses from being reused.
                val nonce = UUID.randomUUID()
                LoginData(
                    loginRedirectUri =
                        UriComponentsBuilder.fromUriString(it)
                            .path("/login")
                            .queryParam("response_type", "code")
                            .queryParam("scope", "openid")
                            .queryParam("client_id", clientId)
                            .queryParam("state", state)
                            .queryParam("nonce", nonce)
                            .queryParam("redirect_uri", redirectUri)
                            .build()
                            .toUri(),
                    nonce = OidcNonce(nonce),
                    state = OidcState(state),
                )
            }
            .onErrorMap {
                OneIdentityConfigurationException("Failed to build login URL: ${it.message}")
            }
    }
}
