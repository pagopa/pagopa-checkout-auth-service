package it.pagopa.checkout.authservice.repositories.redis.bean.auth

import java.util.*

/** Opaque token that uniquely identifies an user session */
data class SessionToken(val sessionToken: UUID) {}
