package it.pagopa.checkout.authservice.services

import it.pagopa.checkout.authservice.clients.oneidentity.OneIdentityClient
import it.pagopa.checkout.authservice.repositories.redis.AuthSessionTokenRepository
import it.pagopa.checkout.authservice.repositories.redis.AuthenticatedUserSessionRepository
import it.pagopa.checkout.authservice.repositories.redis.OIDCAuthStateDataRepository
import it.pagopa.checkout.authservice.repositories.redis.bean.oidc.AuthCode
import it.pagopa.checkout.authservice.repositories.redis.bean.oidc.OidcAuthStateData
import it.pagopa.checkout.authservice.repositories.redis.bean.oidc.OidcState
import it.pagopa.generated.checkout.authservice.v1.model.LoginResponseDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class AuthenticationService(
    private val oneIdentityClient: OneIdentityClient,
    private val oidcAuthStateDataRepository: OIDCAuthStateDataRepository,
    private val authenticatedUserSessionRepository: AuthenticatedUserSessionRepository,
    private val authSessionTokenRepository: AuthSessionTokenRepository
) {

    private val logger = LoggerFactory.getLogger(javaClass)

    fun login(): Mono<LoginResponseDto> =
        oneIdentityClient
            .buildLoginUrl()
            .map {
                val state = it.state
                val nonce = it.nonce
                val redirectionUrl = it.loginRedirectUri
                logger.info("Processing login request for state: [{}] and nonce: [{}] ", state, nonce)
                //save state and nonce association for later validation
                oidcAuthStateDataRepository.save(
                    OidcAuthStateData(
                        state = state,
                        nonce = nonce
                    )
                )
                redirectionUrl
            }
            .map {
                LoginResponseDto()
                    .urlRedirect(it.toString())
            }

    fun retrieveAuthToken(authCode: AuthCode, state: OidcState) {
        logger.info("Retrieving authorization data for auth with state: [{}]", state)
        val oidcAuthState = oidcAuthStateDataRepository
            .findById(state.value.toString())
        if (oidcAuthState == null) {
            TODO("return error for state not found")
        }


    }

}
