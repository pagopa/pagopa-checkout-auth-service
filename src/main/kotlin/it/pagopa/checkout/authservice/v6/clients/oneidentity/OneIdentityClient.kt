package it.pagopa.checkout.authservice.v6.clients.oneidentity

import it.pagopa.checkout.authservice.v6.exception.AuthFailedException
import it.pagopa.checkout.authservice.v6.exception.OneIdentityConfigurationException
import it.pagopa.checkout.authservice.v6.exception.OneIdentityServerException
import it.pagopa.checkout.authservice.v6.repositories.redis.bean.oidc.AuthCode
import it.pagopa.checkout.authservice.v6.repositories.redis.bean.oidc.OidcNonce
import it.pagopa.checkout.authservice.v6.repositories.redis.bean.oidc.OidcState
import it.pagopa.generated.checkout.oneidentity.api.TokenServerApisApi
import it.pagopa.generated.checkout.oneidentity.model.GetJwkSet200ResponseDto
import it.pagopa.generated.checkout.oneidentity.model.TokenDataDto
import java.net.URLEncoder
import java.nio.charset.StandardCharsets
import java.util.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Component
import org.springframework.web.reactive.function.client.WebClientResponseException
import org.springframework.web.util.UriComponentsBuilder
import reactor.core.publisher.Mono

@Component("OneIdentityClientv6")
class OneIdentityClient(
    @Value("\${one-identity.base-url}") private val oneIdentityBaseUrl: String,
    @Value("\${one-identity.redirect-uri}") private val redirectUri: String,
    @Value("\${one-identity.client-id}") private val clientId: String,
    @Value("\${one-identity.client-secret}") private val clientSecret: String,
    @Autowired
    @Qualifier("oneIdentityWebClientv6")
    private val oneIdentityWebClient: TokenServerApisApi,
) {

    private val logger = LoggerFactory.getLogger(javaClass)

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
                val redirectUrlEncoded = URLEncoder.encode(redirectUri, StandardCharsets.UTF_8)
                LoginData(
                    loginRedirectUri =
                        UriComponentsBuilder.fromUriString(it)
                            .path("/login")
                            .queryParam("response_type", "CODE")
                            .queryParam("scope", "openid")
                            .queryParam("client_id", clientId)
                            .queryParam("state", state)
                            .queryParam("nonce", nonce)
                            .queryParam("redirect_uri", redirectUrlEncoded)
                            .build()
                            .toUriString(),
                    nonce = OidcNonce(nonce.toString()),
                    state = OidcState(state.toString()),
                )
            }
            .onErrorMap {
                OneIdentityConfigurationException("Failed to build login URL: ${it.message}")
            }
    }

    fun retrieveOidcToken(authCode: AuthCode, state: OidcState): Mono<TokenDataDto> {
        // basic auth credentials formatting as clientId:clientSecret
        val authorization =
            Base64.getEncoder()
                .encodeToString("$clientId:$clientSecret".toByteArray(StandardCharsets.US_ASCII))
        val code = authCode.value
        val grantType = "AUTHORIZATION_CODE"
        return try {
                oneIdentityWebClient.createRequestToken(
                    authorization, // authorization (basic auth)
                    redirectUri, // redirect uri
                    code, // auth code
                    grantType, // grant type
                )
            } catch (exception: Exception) {
                // openapi generated webclient throws exception in case of input value validation
                // failure instead of returning a Mono error, errors are catch here and mapped to
                // mono error
                Mono.error(exception)
            }
            .onErrorMap {
                val (httpStatusCode, responseBody) =
                    if (it is WebClientResponseException) {
                        Pair(it.statusCode.toString(), it.responseBodyAsString)
                    } else {
                        Pair("N/A", "N/A")
                    }
                logger.error(
                    "Exception retrieving OI id token for OIDC state: [${state.value}], response status code: [$httpStatusCode], response body: [$responseBody]",
                    it,
                )
                when (it) {
                    is WebClientResponseException -> {
                        val errorMessage =
                            "Error retrieving OI id token, http response code: [${it.statusCode}], response message: [${it.responseBodyAsString}]"
                        when (it.statusCode) {
                            // 401-403
                            HttpStatus.UNAUTHORIZED,
                            HttpStatus.FORBIDDEN ->
                                AuthFailedException(
                                    message = errorMessage,
                                    state = state,
                                    cause = it,
                                )
                            // all other http response statuses from the identity provider are
                            // mapped to
                            // 502
                            else ->
                                OneIdentityServerException(
                                    message = errorMessage,
                                    state = state,
                                    cause = it,
                                    status = HttpStatus.BAD_GATEWAY,
                                )
                        }
                    }

                    else ->
                        OneIdentityServerException(
                            state = state,
                            message = "Unhandled error retrieving OI id token: [${it.message}]",
                            cause = it,
                        )
                }
            }
    }

    fun getKeys(): Mono<GetJwkSet200ResponseDto> {
        return try {
                oneIdentityWebClient.jwkSet
            } catch (e: Exception) {
                Mono.error(e)
            }
            .onErrorMap {
                logger.error("Error retrieving jwk set", it)
                OneIdentityConfigurationException("Error while retrieving jwt signing keys")
            }
    }
}
