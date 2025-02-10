package it.pagopa.checkout.authservice.telemetry

import io.opentelemetry.api.GlobalOpenTelemetry
import io.opentelemetry.api.OpenTelemetry
import io.opentelemetry.api.trace.Tracer
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

/**
 * Configuration class for OpenTelemetry setup. Provides the necessary beans for OpenTelemetry
 * tracing functionality in the checkout auth service application.
 */
@Configuration
class OpenTelemetryConfig {

    @Bean
    fun openTelemetry(): OpenTelemetry {
        return GlobalOpenTelemetry.get()
    }

    /**
     * Creates and configures the OpenTelemetry Tracer bean.
     *
     * @param openTelemetry The OpenTelemetry instance injected by Spring
     * @return Configured Tracer instance for the checkout auth service application
     */
    @Bean
    fun tracer(openTelemetry: OpenTelemetry): Tracer {
        return openTelemetry.getTracer("checkout-auth-service", "0.1.0")
    }
}
