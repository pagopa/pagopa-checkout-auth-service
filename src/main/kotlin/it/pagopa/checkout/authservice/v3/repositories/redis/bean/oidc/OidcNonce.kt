package it.pagopa.checkout.authservice.v3.repositories.redis.bean.oidc

/** OIDC authentication flow nonce domain object */
data class OidcNonce(val value: String) {
    init {
        require(value.isNotEmpty() && value.isNotBlank()) {
            "Invalid empty or blank value for nonce"
        }
    }
}
