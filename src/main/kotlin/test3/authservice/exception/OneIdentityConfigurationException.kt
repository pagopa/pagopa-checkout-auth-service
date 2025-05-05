package test3.authservice.exception

import org.springframework.http.HttpStatus

class OneIdentityConfigurationException(message: String) : ApiError(message) {
    override fun toRestException(): RestApiException =
        RestApiException(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "OneIdentity configuration error",
            message!!,
        )
}
