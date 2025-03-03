package it.pagopa.checkout.authservice.services

import it.pagopa.checkout.authservice.clients.oneidentity.OneIdentityClient
import it.pagopa.checkout.authservice.exception.AuthFailedException
import it.pagopa.checkout.authservice.exception.SessionValidationException
import it.pagopa.checkout.authservice.repositories.redis.AuthenticatedUserSessionRepository
import it.pagopa.checkout.authservice.repositories.redis.OIDCAuthStateDataRepository
import it.pagopa.checkout.authservice.repositories.redis.bean.auth.AuthenticatedUserSession
import it.pagopa.checkout.authservice.repositories.redis.bean.auth.Name
import it.pagopa.checkout.authservice.repositories.redis.bean.auth.UserFiscalCode
import it.pagopa.checkout.authservice.repositories.redis.bean.auth.UserInfo
import it.pagopa.checkout.authservice.repositories.redis.bean.oidc.AuthCode
import it.pagopa.checkout.authservice.repositories.redis.bean.oidc.OidcAuthStateData
import it.pagopa.checkout.authservice.repositories.redis.bean.oidc.OidcState
import it.pagopa.checkout.authservice.utils.JwtUtils
import it.pagopa.checkout.authservice.utils.SessionTokenUtils
import it.pagopa.generated.checkout.authservice.v1.model.LoginResponseDto
import it.pagopa.generated.checkout.authservice.v1.model.UserInfoResponseDto
import java.util.*
import org.slf4j.LoggerFactory
import org.springframework.http.server.reactive.ServerHttpRequest
import org.springframework.stereotype.Service
import reactor.core.publisher.Mono

@Service
class AuthenticationService(
    private val oneIdentityClient: OneIdentityClient,
    private val oidcAuthStateDataRepository: OIDCAuthStateDataRepository,
    private val authenticatedUserSessionRepository: AuthenticatedUserSessionRepository,
    private val jwtUtils: JwtUtils,
    private val sessionTokenUtils: SessionTokenUtils,
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
                    state.value,
                    nonce.value,
                )
                // save state and nonce association for later validation
                oidcAuthStateDataRepository.save(OidcAuthStateData(state = state, nonce = nonce))
                redirectionUrl
            }
            .map { LoginResponseDto().urlRedirect(it) }

    fun retrieveAuthToken(authCode: AuthCode, state: OidcState): Mono<AuthenticatedUserSession> {
        logger.info("Retrieving authorization data for auth with state: [{}]", state.value)
        val oidcCachedAuthState =
            Optional.ofNullable(oidcAuthStateDataRepository.findById(state.value))
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
                logger.info("Retrieve id token from OI")
                oneIdentityClient
                    .retrieveOidcToken(authCode = authCode, state = oidcAuthState.state)
                    .flatMap { response -> jwtUtils.validateAndParse(response.idToken) }
                    .flatMap {
                        val nonce = it.get(JwtUtils.OI_JWT_NONCE_CLAIM_KEY, String::class.java)
                        val cachedNonce = oidcAuthState.nonce.value
                        logger.debug(
                            "Cached nonce: [{}], jwt token nonce: [{}]",
                            nonce,
                            cachedNonce,
                        )
                        if (nonce != cachedNonce) {
                            Mono.error(
                                AuthFailedException(
                                    message =
                                        "Nonce mismatch! id token value: [$nonce], cached value: [$cachedNonce]",
                                    state = oidcAuthState.state,
                                )
                            )
                        } else {
                            Mono.just(it)
                        }
                    }
                    .map {
                        val userInfo =
                            UserInfo(
                                name =
                                    Name(
                                        it.get(
                                            JwtUtils.OI_JWT_USER_NAME_CLAIM_KEY,
                                            String::class.java,
                                        )
                                    ),
                                surname =
                                    Name(
                                        it.get(
                                            JwtUtils.OI_JWT_USER_FAMILY_NAME_CLAIM_KEY,
                                            String::class.java,
                                        )
                                    ),
                                fiscalCode =
                                    UserFiscalCode(
                                        it.get(
                                            JwtUtils.OI_JWT_USER_FISCAL_CODE_CLAIM_KEY,
                                            String::class.java,
                                        )
                                    ),
                            )
                        val sessionToken = sessionTokenUtils.generateSessionToken()
                        val authenticatedUserSession =
                            AuthenticatedUserSession(
                                sessionToken = sessionToken,
                                userInfo = userInfo,
                            )
                        // save user logged in information
                        authenticatedUserSessionRepository.save(authenticatedUserSession)
                        authenticatedUserSession
                    }
            }
            .doOnNext {
                logger.info("User logged successfully for state: [{}]", state.value)
                // user logged in, delete authentication state-nonce from cache
                oidcAuthStateDataRepository.delete(state.value)
            }
    }

    fun getUserInfo(request: ServerHttpRequest): Mono<UserInfoResponseDto> {
        return sessionTokenUtils.getSessionTokenFromRequest(request).flatMap { bearerToken ->
            Optional.ofNullable(authenticatedUserSessionRepository.findById(bearerToken))
                .map { authenticatedUserSession ->
                    Mono.just(
                        UserInfoResponseDto(
                            authenticatedUserSession.userInfo.fiscalCode.value,
                            authenticatedUserSession.userInfo.name.value,
                            authenticatedUserSession.userInfo.surname.value,
                        )
                    )
                }
                .orElse(
                    Mono.error(
                        SessionValidationException(message = "Invalid or missing session token")
                    )
                )
        }
    }
}
