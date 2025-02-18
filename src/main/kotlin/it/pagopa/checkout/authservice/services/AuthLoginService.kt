package it.pagopa.checkout.authservice.services

import it.pagopa.checkout.authservice.client.oneidentity.OneIdentityClient
import it.pagopa.generated.checkout.authservice.v1.model.LoginResponseDto
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class AuthLoginService(private val oneIdentityClient: OneIdentityClient) {
    fun login(): Mono<LoginResponseDto> {

        val redirectUrl = oneIdentityClient.buildLoginUrl()

        return Mono.just(LoginResponseDto().urlRedirect(redirectUrl))
    }
}
