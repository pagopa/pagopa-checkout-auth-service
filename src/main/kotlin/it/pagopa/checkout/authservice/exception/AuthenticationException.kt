package it.pagopa.checkout.authservice.exception

import org.springframework.http.HttpStatus

class AuthenticationException(message: String) : ApiError(message) {
    override fun toRestException(): RestApiException =
        RestApiException(HttpStatus.UNAUTHORIZED, "Authentication Failed", message!!)
}
