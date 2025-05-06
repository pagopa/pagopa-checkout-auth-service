package it.pagopa.checkout.authservice.v1.exception

import org.springframework.http.HttpStatus

/** Rest api exception, used to return an error specific HttpStatus and reason code to the caller */
class RestApiException(
    val httpStatus: HttpStatus,
    val title: String,
    val description: String,
    override val cause: Throwable? = null,
) : RuntimeException("[$httpStatus] $title - $description", cause)
