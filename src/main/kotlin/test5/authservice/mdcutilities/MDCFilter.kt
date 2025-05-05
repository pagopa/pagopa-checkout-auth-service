package test5.authservice.mdcutilities

import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
class MDCFilter : WebFilter {
    private val utils = RequestTracingUtils()

    companion object {
        const val HEADER_RPT_IDS = "x-rpt-id"
    }

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val request = exchange.request
        val headers = request.headers
        val method = request.method.name()
        val path = request.uri.path

        // extract header values
        val rptIds =
            headers.getFirst(HEADER_RPT_IDS).takeIf { !it.isNullOrEmpty() }
                ?: RequestTracingUtils.TracingEntry.RPT_IDS.defaultValue

        // create info
        val requestTracingInfo =
            RequestTracingUtils.RequestTracingInfo(
                rptIds = rptIds,
                requestMethod = method,
                requestUriPath = path,
            )

        // add the extracted values to the Reactor context
        return chain.filter(exchange).contextWrite { context ->
            utils.setRequestInfoIntoReactorContext(requestTracingInfo, context)
        }
    }
}
