package it.pagopa.checkout.authservice.exception

import org.springframework.http.HttpStatus

class SessionValidationException(message: String) : ApiError(message) {
    override fun toRestException(): RestApiException =
        RestApiException(
            HttpStatus.UNAUTHORIZED,
            "Unauthorized",
            "Session validation failed: [$message]",
        )
}
