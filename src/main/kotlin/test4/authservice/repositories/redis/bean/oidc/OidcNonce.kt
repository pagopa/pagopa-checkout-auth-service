package test4.authservice.repositories.redis.bean.oidc

/** OIDC authentication flow nonce domain object */
data class OidcNonce(val value: String) {
    init {
        require(value.isNotEmpty() && value.isNotBlank()) {
            "Invalid empty or blank value for nonce"
        }
    }
}
