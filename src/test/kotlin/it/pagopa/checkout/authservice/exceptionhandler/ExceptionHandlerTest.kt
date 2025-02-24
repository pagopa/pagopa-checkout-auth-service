package it.pagopa.checkout.authservice.exceptionhandler

import it.pagopa.checkout.authservice.AuthTestUtils
import it.pagopa.checkout.authservice.exception.AuthenticationException
import it.pagopa.checkout.authservice.exception.OneIdentityClientException
import it.pagopa.checkout.authservice.exception.RestApiException
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus

class ExceptionHandlerTest {

    private val exceptionHandler = ExceptionHandler()

    @Test
    fun `Should handle generic Exception`() {
        val exception = RuntimeException("Unexpected error")
        val response = exceptionHandler.handleGenericException(exception)
        assertEquals(
            AuthTestUtils.buildProblemJson(
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                title = "Internal Server Error",
                description = "An unexpected error occurred processing the request",
            ),
            response.body,
        )
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
    }

    @Test
    fun `Should handle RestApiException`() {
        val response =
            exceptionHandler.handleException(
                RestApiException(
                    httpStatus = HttpStatus.UNAUTHORIZED,
                    title = "title",
                    description = "description",
                )
            )
        assertEquals(
            AuthTestUtils.buildProblemJson(
                httpStatus = HttpStatus.UNAUTHORIZED,
                title = "title",
                description = "description",
            ),
            response.body,
        )
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }

    @Test
    fun `Should handle ApiError`() {
        val exception = AuthenticationException("Authentication Failed")
        val response = exceptionHandler.handleException(exception)

        assertEquals(
            AuthTestUtils.buildProblemJson(
                httpStatus = HttpStatus.UNAUTHORIZED,
                title = "Authentication Failed",
                description = "Authentication Failed",
            ),
            response.body,
        )
        assertEquals(HttpStatus.UNAUTHORIZED, response.statusCode)
    }

    @Test
    fun `Should handle OneIdentityClientException`() {
        val exception = OneIdentityClientException("Connection failed")
        val response = exceptionHandler.handleException(exception)
        assertEquals(
            AuthTestUtils.buildProblemJson(
                httpStatus = HttpStatus.BAD_GATEWAY,
                title = "OneIdentity client error",
                description = "Connection failed",
            ),
            response.body,
        )
        assertEquals(HttpStatus.BAD_GATEWAY, response.statusCode)
    }
}
