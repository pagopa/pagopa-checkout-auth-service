package test3.authservice2.mdcutilities

import org.reactivestreams.Subscription
import org.slf4j.MDC
import reactor.core.CoreSubscriber
import reactor.util.context.Context

class MDCContextLifter<T>(private val coreSubscriber: CoreSubscriber<T>) : CoreSubscriber<T> {
    override fun onSubscribe(subscription: Subscription) {
        coreSubscriber.onSubscribe(subscription)
    }

    override fun onNext(obj: T) {
        copyToMdc(coreSubscriber.currentContext())
        try {
            coreSubscriber.onNext(obj)
        } finally {
            // clear to prevent leaks
            MDC.clear()
        }
    }

    /**
     * Extension function for the Reactor [Context]. Copies the current context to the MDC, if
     * context is empty clears the MDC. State of the MDC after calling this method should be same as
     * Reactor [Context] state. One thread-local access only.
     */
    private fun copyToMdc(context: Context) {
        if (!context.isEmpty) {
            // get current MDC map or create a new one if it doesn't exist
            val mdcContextMap = MDC.getCopyOfContextMap() ?: HashMap()
            // create a map of values from reactor to be copied to MDC
            val reactorContextMap =
                RequestTracingUtils.TracingEntry.entries
                    // for each entry get the context value or the default one
                    .associate { tracingEntry ->
                        val value =
                            context
                                .getOrEmpty<T>(tracingEntry.key)
                                .map { it.toString() }
                                .orElse(tracingEntry.defaultValue)

                        // create k-v pair for the MDC
                        tracingEntry.key to value
                    }
            // add all context values to MDC
            mdcContextMap.putAll(reactorContextMap)
            MDC.setContextMap(mdcContextMap)
        } else {
            // empty context -> clear to avoid stale values
            MDC.clear()
        }
    }

    override fun onError(t: Throwable) {
        try {
            coreSubscriber.onError(t)
        } finally {
            MDC.clear()
        }
    }

    override fun onComplete() {
        try {
            coreSubscriber.onComplete()
        } finally {
            MDC.clear()
        }
    }

    override fun currentContext(): Context {
        return coreSubscriber.currentContext()
    }
}
