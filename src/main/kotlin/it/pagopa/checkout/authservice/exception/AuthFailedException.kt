package it.pagopa.checkout.authservice.exception

import it.pagopa.checkout.authservice.repositories.redis.bean.oidc.OidcState
import org.springframework.http.HttpStatus

class AuthFailedException(
    message: String,
    private val state: OidcState,
    override val cause: Throwable? = null,
) : ApiError("Authentication process error for state: [$state] -> $message") {
    override fun toRestException(): RestApiException =
        RestApiException(
            httpStatus = HttpStatus.UNAUTHORIZED,
            title = "Unauthorized",
            description = "Cannot perform authentication process for state: [$state]",
            cause = cause,
        )
}
