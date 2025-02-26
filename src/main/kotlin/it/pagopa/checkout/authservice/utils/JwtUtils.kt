package it.pagopa.checkout.authservice.utils

import io.jsonwebtoken.Jwts
import java.security.PublicKey
import org.springframework.stereotype.Component

@Component
class JwtUtils {

    fun validateAndParse(jwtToken: String) {
        Jwts.parser()
            .requireIssuer("https://issuer.example.com")
            .verifyWith(null as PublicKey) // TODO retrieve public key
            .build()
            .parse(jwtToken)
    }
}
