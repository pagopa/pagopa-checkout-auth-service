package it.pagopa.checkout.authservice.repositories.redis.bean.auth

import it.pagopa.checkout.authservice.repositories.redis.bean.oidc.AuthCode
import it.pagopa.checkout.authservice.repositories.redis.bean.oidc.OidcNonce
import it.pagopa.checkout.authservice.repositories.redis.bean.oidc.OidcState
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class DomainObjectsTest {

    @ParameterizedTest
    @ValueSource(strings = ["", "   "])
    fun `should throw exception building a Name instance with an invalid value`(
        invalidValue: String
    ) {
        assertThrows<IllegalArgumentException> { Name(invalidValue) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "   "])
    fun `should throw exception building a User Fiscal Code instance with an invalid value`(
        invalidValue: String
    ) {
        assertThrows<IllegalArgumentException> { UserFiscalCode(invalidValue) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["TINIT-fiscalCode", "fiscalCode"])
    fun `should convert UE User Fiscal Code format correctly`(ueFiscalCode: String) {
        assert(UserFiscalCode.fromTinIt(ueFiscalCode).value == "fiscalCode")
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "   "])
    fun `should throw exception building a OidcState instance with an invalid value`(
        invalidValue: String
    ) {
        assertThrows<IllegalArgumentException> { OidcState(invalidValue) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "   "])
    fun `should throw exception building a AuthCode instance with an invalid value`(
        invalidValue: String
    ) {
        assertThrows<IllegalArgumentException> { AuthCode(invalidValue) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["", "   "])
    fun `should throw exception building a OidcNonce instance with an invalid value`(
        invalidValue: String
    ) {
        assertThrows<IllegalArgumentException> { OidcNonce(invalidValue) }
    }

    @Test
    fun `Should build domain objects successfully with valid value`() {
        assertDoesNotThrow { Name("test") }
        assertDoesNotThrow { UserFiscalCode("user fiscal code") }
        assertDoesNotThrow { UserFiscalCode("TINIT-user fiscal code") }
        assertDoesNotThrow { OidcState("oidcState") }
        assertDoesNotThrow { OidcNonce("oidcNonce") }
        assertDoesNotThrow { AuthCode("authCode") }
    }
}
