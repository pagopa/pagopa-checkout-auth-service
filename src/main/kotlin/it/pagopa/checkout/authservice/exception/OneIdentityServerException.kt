package it.pagopa.checkout.authservice.exception

import it.pagopa.checkout.authservice.repositories.redis.bean.oidc.OidcState
import org.springframework.http.HttpStatus

class OneIdentityServerException(message: String, private val state: OidcState) :
    ApiError("Authentication process error for state: [$state] -> $message") {
    override fun toRestException(): RestApiException =
        RestApiException(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "Error communicating with One identity",
            "Cannot perform authentication process for state: [$state]",
        )
}
