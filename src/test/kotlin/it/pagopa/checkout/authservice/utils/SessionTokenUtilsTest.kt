package it.pagopa.checkout.authservice.utils

import java.util.*
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class SessionTokenUtilsTest {

    private val tokenLength = 16
    private val sessionTokenUtils = SessionTokenUtils(tokenLength)

    @Test
    fun `Should generate random generated session token`() {
        val sessionToken1 = sessionTokenUtils.generateSessionToken()
        val sessionToken2 = sessionTokenUtils.generateSessionToken()
        assertTrue(sessionToken1.value != sessionToken2.value)
        assertEquals(tokenLength, Base64.getDecoder().decode(sessionToken1.value).size)
        assertEquals(tokenLength, Base64.getDecoder().decode(sessionToken2.value).size)
    }
}
