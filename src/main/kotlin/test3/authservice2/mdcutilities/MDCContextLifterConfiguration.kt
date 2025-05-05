package test3.authservice2.mdcutilities

import jakarta.annotation.PostConstruct
import jakarta.annotation.PreDestroy
import org.springframework.context.annotation.Configuration
import reactor.core.publisher.Hooks
import reactor.core.publisher.Operators

@Configuration
class MDCContextLifterConfiguration {

    private val mdcContextReactorKey = this::class.java.name

    @PostConstruct
    fun contextOperatorHook() {
        // register a hook applied to each operator in che reactive chain
        Hooks.onEachOperator(
            mdcContextReactorKey,
            // lift -> transforms a Subscriber to apply the MDC lifting functionality
            Operators.lift { _, coreSubscriber ->
                // for each subscriber, wrap it with the MDCContextLifter
                MDCContextLifter(coreSubscriber)
            },
        )
    }

    @PreDestroy
    fun cleanupHook() {
        // remove the hook to prevent memory leaks
        Hooks.resetOnEachOperator(mdcContextReactorKey)
    }
}
