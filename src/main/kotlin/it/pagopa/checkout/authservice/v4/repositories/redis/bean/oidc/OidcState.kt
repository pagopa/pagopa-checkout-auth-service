package it.pagopa.checkout.authservice.v4.repositories.redis.bean.oidc

/** OIDC authentication flow state domain object */
data class OidcState(val value: String) {
    init {
        require(value.isNotEmpty() && value.isNotBlank()) {
            "Invalid empty or blank value for state"
        }
    }
}
