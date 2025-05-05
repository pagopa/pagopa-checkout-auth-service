package it.pagopa.checkout.authservice.v8.repositories.redis.bean.oidc

/**
 * OIDC authentication code that is returned by authentication server during authorization process
 */
data class AuthCode(val value: String) {
    init {
        require(value.isNotEmpty() && value.isNotBlank()) {
            "Invalid value for auth code: [$value]"
        }
    }
}
