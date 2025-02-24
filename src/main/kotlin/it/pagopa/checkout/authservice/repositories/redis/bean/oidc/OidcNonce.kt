package it.pagopa.checkout.authservice.repositories.redis.bean.oidc

import java.util.*

/** OIDC authentication flow nonce domain object */
data class OidcNonce(val value: UUID)
