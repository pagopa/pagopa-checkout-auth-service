package it.pagopa.checkout.authservice.utils

import io.jsonwebtoken.Jwts
import it.pagopa.checkout.authservice.clients.oneidentity.OneIdentityClient
import it.pagopa.checkout.authservice.exception.OneIdentityConfigurationException
import it.pagopa.checkout.authservice.exception.OneIdentityServerException
import it.pagopa.checkout.authservice.repositories.redis.OidcKeysRepository
import it.pagopa.checkout.authservice.repositories.redis.bean.oidc.OidcKey
import it.pagopa.generated.checkout.oneidentity.model.GetJwkSet200ResponseDto
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPublicKey
import java.util.*
import org.junit.jupiter.api.Test
import org.mockito.kotlin.*
import reactor.core.publisher.Hooks
import reactor.core.publisher.Mono
import reactor.test.StepVerifier

class JwtUtilsTest {
    private val oidcKeysRepository: OidcKeysRepository = mock()
    private val oneIdentityClient: OneIdentityClient = mock()
    private val jwtUtils =
        JwtUtils(oidcKeysRepository = oidcKeysRepository, oneIdentityClient = oneIdentityClient)

    @Test
    fun `should validate and parse token successfully getting keys from OI`() {
        // pre-conditions
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(2048)
        val keyPairs =
            listOf(
                keyPairGenerator.generateKeyPair(),
                keyPairGenerator.generateKeyPair(),
                keyPairGenerator.generateKeyPair(),
            )
        val nonce = UUID.randomUUID().toString()
        val userName = "userName"
        val userFamilyName = "userFamilyName"
        val userFiscalCode = "userFiscalCode"
        val oneIdentityResponse =
            GetJwkSet200ResponseDto()
                .keys(
                    keyPairs
                        .map { it.public as RSAPublicKey }
                        .map {
                            mapOf(
                                "kty" to "RSA",
                                "kid" to UUID.randomUUID().toString(),
                                "n" to
                                    Base64.getUrlEncoder().encodeToString(it.modulus.toByteArray()),
                                "e" to
                                    Base64.getUrlEncoder()
                                        .encodeToString(it.publicExponent.toByteArray()),
                            )
                        }
                )

        val signedJwtToken =
            Jwts.builder()
                .signWith(keyPairs[0].private)
                .claim(JwtUtils.OI_JWT_USER_FAMILY_NAME_CLAIM_KEY, userFamilyName)
                .claim(JwtUtils.OI_JWT_USER_NAME_CLAIM_KEY, userName)
                .claim(JwtUtils.OI_JWT_NONCE_CLAIM_KEY, nonce)
                .claim(JwtUtils.OI_JWT_USER_FISCAL_CODE_CLAIM_KEY, userFiscalCode)
                .compact()
        given(oidcKeysRepository.getAllValues()).willReturn(emptyList())
        given(oneIdentityClient.getKeys()).willReturn(Mono.just(oneIdentityResponse))
        doNothing().`when`(oidcKeysRepository).save(any())
        Hooks.onOperatorDebug()
        // test
        val expectedClaims = Jwts.claims()
        val expectedSavedKeys =
            oneIdentityResponse.keys.map {
                OidcKey(kid = it["kid"]!!, n = it["n"]!!, e = it["e"]!!)
            }
        expectedClaims["name"] = userName
        expectedClaims["familyName"] = userFamilyName
        expectedClaims["fiscalNumber"] = userFiscalCode
        expectedClaims["nonce"] = nonce
        StepVerifier.create(jwtUtils.validateAndParse(signedJwtToken))
            .expectNext(expectedClaims)
            .verifyComplete()
        verify(oidcKeysRepository, times(1)).getAllValues()
        verify(oneIdentityClient, times(1)).getKeys()
        expectedSavedKeys.forEach { verify(oidcKeysRepository, times(1)).save(it) }
        verify(oidcKeysRepository, times(0)).deleteAll()
    }

    @Test
    fun `should validate and parse token successfully retrieving keys from cache`() {
        // pre-conditions
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(2048)
        val keyPairs =
            listOf(
                keyPairGenerator.generateKeyPair(),
                keyPairGenerator.generateKeyPair(),
                keyPairGenerator.generateKeyPair(),
            )
        val nonce = UUID.randomUUID().toString()
        val userName = "userName"
        val userFamilyName = "userFamilyName"
        val userFiscalCode = "userFiscalCode"
        val cachedKeys =
            keyPairs
                .map { it.public as RSAPublicKey }
                .map {
                    OidcKey(
                        kid = UUID.randomUUID().toString(),
                        n = Base64.getUrlEncoder().encodeToString(it.modulus.toByteArray()),
                        e = Base64.getUrlEncoder().encodeToString(it.publicExponent.toByteArray()),
                    )
                }
        val signedJwtToken =
            Jwts.builder()
                .signWith(keyPairs[0].private)
                .claim(JwtUtils.OI_JWT_USER_FAMILY_NAME_CLAIM_KEY, userFamilyName)
                .claim(JwtUtils.OI_JWT_USER_NAME_CLAIM_KEY, userName)
                .claim(JwtUtils.OI_JWT_NONCE_CLAIM_KEY, nonce)
                .claim(JwtUtils.OI_JWT_USER_FISCAL_CODE_CLAIM_KEY, userFiscalCode)
                .compact()
        given(oidcKeysRepository.getAllValues()).willReturn(cachedKeys)
        Hooks.onOperatorDebug()
        // test
        val expectedClaims = Jwts.claims()
        expectedClaims["name"] = userName
        expectedClaims["familyName"] = userFamilyName
        expectedClaims["fiscalNumber"] = userFiscalCode
        expectedClaims["nonce"] = nonce
        StepVerifier.create(jwtUtils.validateAndParse(signedJwtToken))
            .expectNext(expectedClaims)
            .verifyComplete()
        verify(oidcKeysRepository, times(1)).getAllValues()
        verify(oneIdentityClient, times(0)).getKeys()
        verify(oidcKeysRepository, times(0)).save(any())
        verify(oidcKeysRepository, times(0)).deleteAll()
    }

    @Test
    fun `should return error for invalid key response received from OI`() {
        // pre-conditions
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(2048)
        val keyPairs =
            listOf(
                keyPairGenerator.generateKeyPair(),
                keyPairGenerator.generateKeyPair(),
                keyPairGenerator.generateKeyPair(),
            )
        val nonce = UUID.randomUUID().toString()
        val userName = "userName"
        val userFamilyName = "userFamilyName"
        val userFiscalCode = "userFiscalCode"
        val oneIdentityResponse =
            GetJwkSet200ResponseDto()
                .keys(
                    keyPairs
                        .map { it.public as RSAPublicKey }
                        .map {
                            mapOf(
                                "kty" to "RSA",
                                "kid" to UUID.randomUUID().toString(),
                                "e" to
                                    Base64.getUrlEncoder()
                                        .encodeToString(it.publicExponent.toByteArray()),
                                // invalid response, does not contains RSA key modulus
                            )
                        }
                )

        val signedJwtToken =
            Jwts.builder()
                .signWith(keyPairs[0].private)
                .claim(JwtUtils.OI_JWT_USER_FAMILY_NAME_CLAIM_KEY, userFamilyName)
                .claim(JwtUtils.OI_JWT_USER_NAME_CLAIM_KEY, userName)
                .claim(JwtUtils.OI_JWT_NONCE_CLAIM_KEY, nonce)
                .claim(JwtUtils.OI_JWT_USER_FISCAL_CODE_CLAIM_KEY, userFiscalCode)
                .compact()
        given(oidcKeysRepository.getAllValues()).willReturn(emptyList())
        given(oneIdentityClient.getKeys()).willReturn(Mono.just(oneIdentityResponse))
        doNothing().`when`(oidcKeysRepository).save(any())
        Hooks.onOperatorDebug()
        // test
        val expectedClaims = Jwts.claims()
        expectedClaims["name"] = userName
        expectedClaims["familyName"] = userFamilyName
        expectedClaims["fiscalNumber"] = userFiscalCode
        expectedClaims["nonce"] = nonce
        StepVerifier.create(jwtUtils.validateAndParse(signedJwtToken))
            .expectError(OneIdentityServerException::class.java)
            .verify()
        verify(oidcKeysRepository, times(1)).getAllValues()
        verify(oneIdentityClient, times(1)).getKeys()
        verify(oidcKeysRepository, times(0)).save(any())
        verify(oidcKeysRepository, times(0)).deleteAll()
    }

    @Test
    fun `should return error for no RSA key returned by OI`() {
        // pre-conditions
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(2048)
        val keyPairs =
            listOf(
                keyPairGenerator.generateKeyPair(),
                keyPairGenerator.generateKeyPair(),
                keyPairGenerator.generateKeyPair(),
            )
        val nonce = UUID.randomUUID().toString()
        val userName = "userName"
        val userFamilyName = "userFamilyName"
        val userFiscalCode = "userFiscalCode"
        val oneIdentityResponse = GetJwkSet200ResponseDto().keys(listOf())

        val signedJwtToken =
            Jwts.builder()
                .signWith(keyPairs[0].private)
                .claim(JwtUtils.OI_JWT_USER_FAMILY_NAME_CLAIM_KEY, userFamilyName)
                .claim(JwtUtils.OI_JWT_USER_NAME_CLAIM_KEY, userName)
                .claim(JwtUtils.OI_JWT_NONCE_CLAIM_KEY, nonce)
                .claim(JwtUtils.OI_JWT_USER_FISCAL_CODE_CLAIM_KEY, userFiscalCode)
                .compact()
        given(oidcKeysRepository.getAllValues()).willReturn(emptyList())
        given(oneIdentityClient.getKeys()).willReturn(Mono.just(oneIdentityResponse))
        doNothing().`when`(oidcKeysRepository).save(any())
        Hooks.onOperatorDebug()
        // test
        val expectedClaims = Jwts.claims()
        val expectedSavedKeys =
            oneIdentityResponse.keys.map {
                OidcKey(kid = it["kid"]!!, n = it["n"]!!, e = it["e"]!!)
            }
        expectedClaims["name"] = userName
        expectedClaims["familyName"] = userFamilyName
        expectedClaims["fiscalNumber"] = userFiscalCode
        expectedClaims["nonce"] = nonce
        StepVerifier.create(jwtUtils.validateAndParse(signedJwtToken))
            .expectError(OneIdentityConfigurationException::class.java)
            .verify()
        verify(oidcKeysRepository, times(1)).getAllValues()
        verify(oneIdentityClient, times(1)).getKeys()
        expectedSavedKeys.forEach { verify(oidcKeysRepository, times(1)).save(it) }
        verify(oidcKeysRepository, times(0)).deleteAll()
    }

    @Test
    fun `should throw error for input token whose signature cannot be verified`() {
        // pre-conditions
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(2048)
        val keyPairs =
            listOf(
                keyPairGenerator.generateKeyPair(),
                keyPairGenerator.generateKeyPair(),
                keyPairGenerator.generateKeyPair(),
            )
        val nonce = UUID.randomUUID().toString()
        val userName = "userName"
        val userFamilyName = "userFamilyName"
        val userFiscalCode = "userFiscalCode"
        val oneIdentityResponse =
            GetJwkSet200ResponseDto()
                .keys(
                    keyPairs
                        .stream()
                        .skip(
                            1
                        ) // skip the first key, that is the one used to sign the input test token
                        .map { it.public as RSAPublicKey }
                        .map {
                            mapOf(
                                "kty" to "RSA",
                                "kid" to UUID.randomUUID().toString(),
                                "n" to
                                    Base64.getUrlEncoder().encodeToString(it.modulus.toByteArray()),
                                "e" to
                                    Base64.getUrlEncoder()
                                        .encodeToString(it.publicExponent.toByteArray()),
                            )
                        }
                        .toList()
                )
        val cachedKeys = setOf("test1", "test2")
        val signedJwtToken =
            Jwts.builder()
                .signWith(keyPairs[0].private)
                .claim(JwtUtils.OI_JWT_USER_FAMILY_NAME_CLAIM_KEY, userFamilyName)
                .claim(JwtUtils.OI_JWT_USER_NAME_CLAIM_KEY, userName)
                .claim(JwtUtils.OI_JWT_NONCE_CLAIM_KEY, nonce)
                .claim(JwtUtils.OI_JWT_USER_FISCAL_CODE_CLAIM_KEY, userFiscalCode)
                .compact()
        given(oidcKeysRepository.getAllValues()).willReturn(emptyList())
        given(oneIdentityClient.getKeys()).willReturn(Mono.just(oneIdentityResponse))
        given(oidcKeysRepository.deleteAll()).willReturn(cachedKeys.size.toLong())
        doNothing().`when`(oidcKeysRepository).save(any())
        Hooks.onOperatorDebug()
        // test
        val expectedClaims = Jwts.claims()
        val expectedSavedKeys =
            oneIdentityResponse.keys.map {
                OidcKey(kid = it["kid"]!!, n = it["n"]!!, e = it["e"]!!)
            }
        expectedClaims["name"] = userName
        expectedClaims["familyName"] = userFamilyName
        expectedClaims["fiscalNumber"] = userFiscalCode
        expectedClaims["nonce"] = nonce
        StepVerifier.create(jwtUtils.validateAndParse(signedJwtToken))
            .expectError(NoSuchElementException::class.java)
            .verify()
        verify(oidcKeysRepository, times(1)).getAllValues()
        verify(oneIdentityClient, times(1)).getKeys()
        expectedSavedKeys.forEach { verify(oidcKeysRepository, times(1)).save(it) }
        verify(oidcKeysRepository, times(1)).deleteAll()
    }
}
