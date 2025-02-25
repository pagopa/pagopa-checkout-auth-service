package it.pagopa.checkout.authservice.exceptionhandler

import it.pagopa.checkout.authservice.AuthTestUtils
import it.pagopa.checkout.authservice.exception.OneIdentityConfigurationException
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
    fun `Should handle OneIdentityConfigurationException`() {
        val exception = OneIdentityConfigurationException("Connection failed")
        val response = exceptionHandler.handleException(exception)
        assertEquals(
            AuthTestUtils.buildProblemJson(
                httpStatus = HttpStatus.INTERNAL_SERVER_ERROR,
                title = "OneIdentity configuration error",
                description = "Connection failed",
            ),
            response.body,
        )
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.statusCode)
    }
}
