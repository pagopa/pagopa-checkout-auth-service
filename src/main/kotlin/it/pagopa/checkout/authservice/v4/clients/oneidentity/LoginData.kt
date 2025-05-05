package it.pagopa.checkout.authservice.v4.clients.oneidentity

import it.pagopa.checkout.authservice.v4.repositories.redis.bean.oidc.OidcNonce
import it.pagopa.checkout.authservice.v4.repositories.redis.bean.oidc.OidcState

/** Login operation result data */
data class LoginData(val loginRedirectUri: String, val state: OidcState, val nonce: OidcNonce)
