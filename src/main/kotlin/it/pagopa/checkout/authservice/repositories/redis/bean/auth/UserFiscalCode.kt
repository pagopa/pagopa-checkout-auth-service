package it.pagopa.checkout.authservice.repositories.redis.bean.auth

/** User fiscal code domain object */
data class UserFiscalCode(val value: String) {

    init {
        require(value.isNotEmpty() && value.isNotBlank()) {
            "Invalid blank or empty value for user fiscal code"
        }
    }
}
