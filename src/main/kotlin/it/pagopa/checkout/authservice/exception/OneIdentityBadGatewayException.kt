package it.pagopa.checkout.authservice.exception

import it.pagopa.checkout.authservice.repositories.redis.bean.oidc.OidcState
import org.springframework.http.HttpStatus

class OneIdentityBadGatewayException(
    message: String,
    private val state: OidcState? = null,
    override val cause: Throwable? = null,
) : ApiError("Authentication process error for state: [${state?.value ?: "N/A"}] -> $message") {
    override fun toRestException(): RestApiException =
        RestApiException(
            httpStatus = HttpStatus.BAD_GATEWAY,
            title = "Error communicating with One identity",
            description =
            "Cannot perform authentication process for state: [${state?.value ?: "N/A"}]",
            cause = cause,
        )
}