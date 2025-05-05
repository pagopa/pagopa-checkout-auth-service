package test.authservice2.repositories.redis.bean.oidc

import org.springframework.data.annotation.Id

/**
 * OIDC authentication state data: contains authentication state data such as nonce, that are used
 * to validate authentication flow
 */
data class OidcAuthStateData(@Id val state: OidcState, val nonce: OidcNonce)
