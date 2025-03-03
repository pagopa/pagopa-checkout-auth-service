package it.pagopa.checkout.authservice.controllers

import it.pagopa.checkout.authservice.repositories.redis.bean.oidc.AuthCode
import it.pagopa.checkout.authservice.repositories.redis.bean.oidc.OidcState
import it.pagopa.checkout.authservice.services.AuthenticationService
import it.pagopa.generated.checkout.authservice.v1.api.AuthApi
import it.pagopa.generated.checkout.authservice.v1.model.AuthRequestDto
import it.pagopa.generated.checkout.authservice.v1.model.AuthResponseDto
import it.pagopa.generated.checkout.authservice.v1.model.LoginResponseDto
import it.pagopa.generated.checkout.authservice.v1.model.UserInfoResponseDto
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono

@RestController
class AuthLoginController(@Autowired private val authenticationService: AuthenticationService) :
    AuthApi {
    private val logger = LoggerFactory.getLogger(this.javaClass)

    /**
     * GET /auth/login : Login endpoint GET login endpoint
     *
     * @return Successful login (status code 200) or Formally invalid input (status code 400) or
     *   User not found (status code 404) or Internal server error (status code 500)
     */
    override fun authLogin(
        xRptId: String?,
        exchange: ServerWebExchange?,
    ): Mono<ResponseEntity<LoginResponseDto>> {
        logger.info("Received login request for rptId [{}]", xRptId)
        return authenticationService.login().map { loginResponse ->
            ResponseEntity.ok(loginResponse)
        }
    }

    /**
     * POST /auth/logout : Logout endpoint POST logout endpoint
     *
     * @return Successful logout (status code 204) or Formally invalid input (status code 400) or
     *   Unauthorized (status code 401) or User not found (status code 404) or Internal server error
     *   (status code 500)
     */
    override fun authLogout(exchange: ServerWebExchange?): Mono<ResponseEntity<Void>> {
        return Mono.just(ResponseEntity.status(HttpStatus.NOT_IMPLEMENTED).build())
    }

    /**
     * GET /auth/users : Get user information GET user information
     *
     * @return Successful retrieval of user information (status code 200) or Formally invalid input
     *   (status code 400) or Unauthorized (status code 401) or User not found (status code 404) or
     *   Internal server error (status code 500)
     */
    override fun authUsers(exchange: ServerWebExchange): Mono<ResponseEntity<UserInfoResponseDto>> {
        return authenticationService.getUserInfo(exchange.request).map { userInfo ->
            ResponseEntity.ok(userInfo)
        }
    }

    /**
     * POST /auth/token : Authentication endpoint POST authentication endpoint with auth code
     *
     * @param authRequestDto (required)
     * @return Successful authentication (status code 200) or Formally invalid input (status
     *   code 400) or Unauthorized (status code 401) or User not found (status code 404) or Internal
     *   server error (status code 500)
     */
    override fun authenticateWithAuthToken(
        authRequestDto: Mono<AuthRequestDto>,
        exchange: ServerWebExchange,
    ): Mono<ResponseEntity<AuthResponseDto>> =
        authRequestDto
            .flatMap {
                authenticationService.retrieveAuthToken(
                    authCode = AuthCode(it.authCode),
                    state = OidcState(it.state),
                )
            }
            .map { ResponseEntity.ok().body(AuthResponseDto().authToken(it.sessionToken.value)) }

    /**
     * GET /auth/validate : Validate a token GET endpoint to validate a token
     *
     * @return Token is valid (status code 200) or Invalid token (status code 400) or Unauthorized
     *   (status code 401) or Internal server error (status code 500)
     */
    override fun validateToken(exchange: ServerWebExchange): Mono<ResponseEntity<Void>> {
        return authenticationService.validateAuthToken(exchange.request).map {
            ResponseEntity.ok().build()
        }
    }
}
