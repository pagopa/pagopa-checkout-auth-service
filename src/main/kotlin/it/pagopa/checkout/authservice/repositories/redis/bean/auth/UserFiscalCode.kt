package it.pagopa.checkout.authservice.repositories.redis.bean.auth

/** User fiscal code domain object */
data class UserFiscalCode(val value: String) {

    init {
        require(value.length == 16) { "Invalid value for fiscal code, must be 16 chars length" }
    }
}
