package it.pagopa.checkout.authservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication class PagopaCheckoutAuthserviceApplication

fun main(args: Array<String>) {
    runApplication<PagopaCheckoutAuthserviceApplication>(*args)
}
