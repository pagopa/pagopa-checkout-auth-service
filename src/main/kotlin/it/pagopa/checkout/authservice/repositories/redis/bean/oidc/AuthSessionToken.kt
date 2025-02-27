package it.pagopa.checkout.authservice.repositories.redis.bean.oidc

import it.pagopa.checkout.authservice.repositories.redis.bean.auth.SessionToken

/** This entity maps OIDC auth code to a checkout auth session token */
data class AuthSessionToken(val authCode: AuthCode, val sessionToken: SessionToken)
