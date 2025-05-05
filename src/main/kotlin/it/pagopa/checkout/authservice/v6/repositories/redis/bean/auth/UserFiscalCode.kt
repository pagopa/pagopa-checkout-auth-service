package it.pagopa.checkout.authservice.v6.repositories.redis.bean.auth

/** User fiscal code domain object */
data class UserFiscalCode(val value: String) {
    companion object {
        const val OIDC_SPID = "TINIT-"

        fun fromTinIt(value: String): UserFiscalCode {
            return UserFiscalCode(value.removePrefix(OIDC_SPID))
        }
    }

    init {
        require(value.isNotEmpty() && value.isNotBlank()) {
            "Invalid blank or empty value for user fiscal code"
        }
    }
}
