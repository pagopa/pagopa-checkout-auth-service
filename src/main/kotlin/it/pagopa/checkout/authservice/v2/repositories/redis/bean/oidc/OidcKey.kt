package it.pagopa.checkout.authservice.v2.repositories.redis.bean.oidc

import org.springframework.data.annotation.Id

/** Data class used to cache OIDC keys */
data class OidcKey(@Id val kid: String, val n: String, val e: String)
