package test5.authservice.exception

import it.pagopa.checkout.authservice.repositories.redis.bean.oidc.OidcState
import org.springframework.http.HttpStatus

class OneIdentityServerException(
    message: String,
    private val state: OidcState? = null,
    override val cause: Throwable? = null,
    private val status: HttpStatus = HttpStatus.BAD_GATEWAY,
) : ApiError("Authentication process error for state: [${state?.value ?: "N/A"}] -> $message") {
    override fun toRestException(): RestApiException =
        RestApiException(
            httpStatus = status,
            title = "Error communicating with One identity",
            description =
                "Cannot perform authentication process for state: [${state?.value ?: "N/A"}]",
            cause = cause,
        )
}
