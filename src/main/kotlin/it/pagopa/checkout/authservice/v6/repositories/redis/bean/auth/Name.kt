package it.pagopa.checkout.authservice.v6.repositories.redis.bean.auth

/** An user name/surname domain object */
data class Name(val value: String) {

    init {
        require(value.isNotBlank() && value.isNotEmpty()) {
            "Invalid blank or empty value for name"
        }
    }
}
