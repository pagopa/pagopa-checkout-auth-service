package it.pagopa.checkout.authservice.exception

import org.springframework.http.HttpStatus

class OneIdentityClientException(message: String) : ApiError(message) {
    override fun toRestException(): RestApiException =
        RestApiException(HttpStatus.BAD_GATEWAY, "OneIdentity client error", message!!)
}
