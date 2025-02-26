package it.pagopa.checkout.authservice.services

import it.pagopa.checkout.authservice.clients.oneidentity.OneIdentityClient
import it.pagopa.checkout.authservice.exception.AuthFailedException
import it.pagopa.checkout.authservice.repositories.redis.AuthSessionTokenRepository
import it.pagopa.checkout.authservice.repositories.redis.AuthenticatedUserSessionRepository
import it.pagopa.checkout.authservice.repositories.redis.OIDCAuthStateDataRepository
import it.pagopa.checkout.authservice.repositories.redis.bean.auth.Name
import it.pagopa.checkout.authservice.repositories.redis.bean.auth.UserFiscalCode
import it.pagopa.checkout.authservice.repositories.redis.bean.auth.UserInfo
import it.pagopa.checkout.authservice.repositories.redis.bean.oidc.AuthCode
import it.pagopa.checkout.authservice.repositories.redis.bean.oidc.OidcAuthStateData
import it.pagopa.checkout.authservice.repositories.redis.bean.oidc.OidcState
import it.pagopa.checkout.authservice.utils.JwtUtils
import it.pagopa.generated.checkout.authservice.v1.model.LoginResponseDto
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono
import java.util.*

@Service
class AuthenticationService(
    private val oneIdentityClient: OneIdentityClient,
    private val oidcAuthStateDataRepository: OIDCAuthStateDataRepository,
    private val authenticatedUserSessionRepository: AuthenticatedUserSessionRepository,
    private val authSessionTokenRepository: AuthSessionTokenRepository,
    private val jwtUtils: JwtUtils
) {


    private val logger = LoggerFactory.getLogger(javaClass)

    fun login(): Mono<LoginResponseDto> =
        oneIdentityClient
            .buildLoginUrl()
            .map {
                val state = it.state
                val nonce = it.nonce
                val redirectionUrl = it.loginRedirectUri
                logger.info(
                    "Processing login request for state: [{}] and nonce: [{}] ",
                    state,
                    nonce,
                )
                // save state and nonce association for later validation
                oidcAuthStateDataRepository.save(OidcAuthStateData(state = state, nonce = nonce))
                redirectionUrl
            }
            .map { LoginResponseDto().urlRedirect(it.toString()) }

    fun retrieveAuthToken(authCode: AuthCode, state: OidcState): Mono<UserInfo> {
        logger.info("Retrieving authorization data for auth with state: [{}]", state)
        val oidcCachedAuthState =
            Optional
                .ofNullable(oidcAuthStateDataRepository.findById(state.value.toString()))
                .map { Mono.just(it) }
                .orElse(
                    Mono.error(
                        AuthFailedException(
                            state = state,
                            message = "Cannot retrieve OIDC session for input auth state",
                        )
                    )
                )
        return oidcCachedAuthState
            .flatMap { oidcAuthState ->
                oneIdentityClient.retrieveOidcToken(authCode = authCode, state = oidcAuthState.state)
                    .flatMap { response ->
                        jwtUtils.validateAndParse(response.idToken)
                    }
                    .filter {
                        val nonce = it.payload.get(JwtUtils.OI_JWT_USER_NAME_CLAIM_KEY, String::class.java)
                        logger.debug("Cached nonce: π")
                        nonce == oidcAuthState.nonce.value.toString()
                    }
                    .map {
                        UserInfo(
                            name = Name(
                                it.payload.get(JwtUtils.OI_JWT_USER_NAME_CLAIM_KEY, String::class.java)
                            ),
                            surname = Name(
                                it.payload.get(JwtUtils.OI_JWT_USER_FAMILY_NAME_CLAIM_KEY, String::class.java)
                            ),
                            fiscalCode = UserFiscalCode(
                                it.payload.get(
                                    JwtUtils.OI_JWT_USER_FISCAL_CODE_CLAIM_KEY,
                                    String::class.java
                                )
                            )
                        )
                    }
            }
    }
}
