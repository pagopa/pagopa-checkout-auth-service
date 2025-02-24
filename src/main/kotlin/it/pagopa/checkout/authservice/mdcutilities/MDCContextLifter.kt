package it.pagopa.checkout.authservice.mdcutilities

import org.reactivestreams.Subscription
import reactor.core.CoreSubscriber

class MDCContextLifter<T>(private val coreSubscriber: CoreSubscriber<T>) : CoreSubscriber<T> {
    override fun onSubscribe(p0: Subscription) {
        TODO("Not yet implemented")
    }

    override fun onNext(p0: T?) {
        TODO("Not yet implemented")
    }

    override fun onError(p0: Throwable?) {
        TODO("Not yet implemented")
    }

    override fun onComplete() {
        TODO("Not yet implemented")
    }
}
