package it.pagopa.checkout.authservice.clients.oneidentity

import it.pagopa.checkout.authservice.exception.OneIdentityConfigurationException
import it.pagopa.checkout.authservice.repositories.redis.bean.oidc.AuthCode
import it.pagopa.checkout.authservice.repositories.redis.bean.oidc.OidcNonce
import it.pagopa.checkout.authservice.repositories.redis.bean.oidc.OidcState
import it.pagopa.generated.checkout.oneidentity.api.TokenServerApisApi
import it.pagopa.generated.checkout.oneidentity.model.TokenDataDto
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono
import java.util.*

@Component
class OneIdentityClient(
    @Value("\${one-identity.base-url}") private val oneIdentityBaseUrl: String,
    @Value("\${one-identity.redirect-uri}") private val redirectUri: String,
    @Value("\${one-identity.client-id}") private val clientId: String,
    private val oneIdentityServerApisApi: TokenServerApisApi
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

    fun retrieveOidcToken(authCode: AuthCode): Mono<TokenDataDto> {
        val authorization = ""
        val redirectUri = ""
        val code = authCode.value
        val grantType = "AUTHORIZATION_CODE"
        return try {
            oneIdentityServerApisApi.createRequestToken(
                authorization,//authorization (basic auth)
                redirectUri,//redirect uri
                code,//auth code
                grantType//grant type
            )
        } catch (exception: WebClientResponseException) {
            //openapi generated webclient throws exception in case of input value validation failure
            Mono.error(exception)
        }
    }
}
