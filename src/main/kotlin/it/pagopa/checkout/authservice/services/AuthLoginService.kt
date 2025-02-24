package it.pagopa.checkout.authservice.services

import it.pagopa.checkout.authservice.client.oneidentity.OneIdentityClient
import it.pagopa.checkout.authservice.exception.OneIdentityClientException
import it.pagopa.generated.checkout.authservice.v1.model.LoginResponseDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class AuthLoginService(private val oneIdentityClient: OneIdentityClient) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun login(rptId: String): Mono<LoginResponseDto> {
        logger.info("Starting login process related to RPTID: [{}]", rptId)

        return oneIdentityClient.buildLoginUrl().map { url ->
            if (url.isBlank()) {
                throw OneIdentityClientException("Generated URL is empty")
            }
            LoginResponseDto().urlRedirect(url)
        }
    }
}
