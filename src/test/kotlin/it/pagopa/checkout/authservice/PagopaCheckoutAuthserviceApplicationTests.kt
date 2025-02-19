package it.pagopa.checkout.authservice

import kotlin.test.assertTrue
import org.junit.jupiter.api.Test
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.TestPropertySource

@SpringBootTest
@TestPropertySource(locations = ["classpath:application.test.properties"])
class PagopaCheckoutAuthserviceApplicationTests {

    @Test
    fun contextLoads() {
        assertTrue { true }
    }
}
