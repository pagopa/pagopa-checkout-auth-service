package it.pagopa.checkout.authservice.telemetry

import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Tracer
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.AnnotationConfigApplicationContext

class OpenTelemetryConfigTest {

    @Test
    fun `test OpenTelemetry and Tracer beans are created`() {
        // Create a Spring application context with the OpenTelemetryConfig
        val context = AnnotationConfigApplicationContext(OpenTelemetryConfig::class.java)

        // Retrieve the OpenTelemetry bean
        val openTelemetry = context.getBean(OpenTelemetry::class.java)
        assertNotNull(openTelemetry, "OpenTelemetry bean should not be null")

        // Retrieve the Tracer bean
        val tracer = context.getBean(Tracer::class.java)
        assertNotNull(tracer, "Tracer bean should not be null")

        // Verify that the tracer is working by creating a span
        val span = tracer.spanBuilder("span-test").startSpan()
        assertNotNull(span, "Span should not be null")

        // End the span
        span.end()

        // Close the context
        context.close()
    }
}
