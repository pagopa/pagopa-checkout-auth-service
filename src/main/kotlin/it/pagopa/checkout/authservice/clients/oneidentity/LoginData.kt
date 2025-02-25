package it.pagopa.checkout.authservice.clients.oneidentity

import it.pagopa.checkout.authservice.repositories.redis.bean.oidc.OidcNonce
import it.pagopa.checkout.authservice.repositories.redis.bean.oidc.OidcState
import java.net.URI

/** Login operation result data */
data class LoginData(val loginRedirectUri: URI, val state: OidcState, val nonce: OidcNonce)
