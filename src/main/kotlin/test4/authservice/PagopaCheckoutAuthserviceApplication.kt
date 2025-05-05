package test4.authservice

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication
import reactor.core.publisher.Hooks

@SpringBootApplication class PagopaCheckoutAuthserviceApplication

fun main(args: Array<String>) {
    Hooks.enableAutomaticContextPropagation()
    runApplication<PagopaCheckoutAuthserviceApplication>(*args)
}
