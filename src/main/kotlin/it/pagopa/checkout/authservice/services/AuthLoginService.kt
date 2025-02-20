package it.pagopa.checkout.authservice.services

import it.pagopa.checkout.authservice.client.oneidentity.OneIdentityClient
import it.pagopa.generated.checkout.authservice.v1.model.LoginResponseDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class AuthLoginService(private val oneIdentityClient: OneIdentityClient) {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    fun login(noticeNumber: String): Mono<LoginResponseDto> {
        logger.info("Starting login process related to noticeNumber: [{}]", noticeNumber)

        return try {
            val redirectUrl = oneIdentityClient.buildLoginUrl()
            logger.debug("Login URL successfully built related to noticeNumber: [{}]", noticeNumber)
            Mono.just(LoginResponseDto().urlRedirect(redirectUrl)).doOnSuccess {
                logger.info(
                    "Login process related to noticeNumber: [{}] completed successfully",
                    noticeNumber,
                )
            }
        } catch (e: Exception) {
            logger.error(
                "Error building login URL for noticeNumber: [{}] with error: {}",
                noticeNumber,
                e.message,
            )
            Mono.error(e)
        }
    }
}
