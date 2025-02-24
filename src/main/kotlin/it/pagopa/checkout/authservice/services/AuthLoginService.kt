package it.pagopa.checkout.authservice.services

import it.pagopa.checkout.authservice.clients.oneidentity.OneIdentityClient
import it.pagopa.checkout.authservice.exception.OneIdentityClientException
import it.pagopa.generated.checkout.authservice.v1.model.LoginResponseDto
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class AuthLoginService(private val oneIdentityClient: OneIdentityClient) {

    fun login(): Mono<LoginResponseDto> {

        return oneIdentityClient.buildLoginUrl().map { url ->
            if (url.isBlank()) {
                throw OneIdentityClientException("Generated URL is empty")
            }
            LoginResponseDto().urlRedirect(url)
        }
    }
}
