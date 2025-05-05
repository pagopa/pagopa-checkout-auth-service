package it.pagopa.checkout.authservice.v1.repositories.redis.bean.auth

/** Opaque token that uniquely identifies an user session */
data class SessionToken(val value: String) {}
