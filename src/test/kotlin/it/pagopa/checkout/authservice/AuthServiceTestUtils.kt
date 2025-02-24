package it.pagopa.checkout.authservice

import it.pagopa.generated.checkout.authservice.v1.model.ProblemJsonDto
import org.springframework.http.HttpStatus

object AuthTestUtils {
    fun buildProblemJson(
        httpStatus: HttpStatus,
        title: String,
        description: String,
    ): ProblemJsonDto = ProblemJsonDto().status(httpStatus.value()).title(title).detail(description)
}
