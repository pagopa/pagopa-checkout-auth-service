package it.pagopa.checkout.authservice.v3.repositories.redis.bean.auth

import org.springframework.data.annotation.Id

/** Authenticated user session data - contains authentication data associated to a session token */
data class AuthenticatedUserSession(@Id val sessionToken: SessionToken, val userInfo: UserInfo)
