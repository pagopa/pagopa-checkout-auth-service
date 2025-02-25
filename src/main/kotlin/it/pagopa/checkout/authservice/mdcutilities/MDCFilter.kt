package it.pagopa.checkout.authservice.mdcutilities

import org.springframework.stereotype.Component
import org.springframework.web.server.ServerWebExchange
import org.springframework.web.server.WebFilter
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono

@Component
class MDCFilter : WebFilter {

    companion object {
        const val HEADER_FORWARDED_FOR = "x-forwarded-for"
        const val HEADER_RPT_ID = "x-rptid"
    }

    override fun filter(exchange: ServerWebExchange, chain: WebFilterChain): Mono<Void> {
        val request = exchange.request
        val headers = request.headers
        val method = request.method.name()
        val path = request.uri.path

        // debug logging
        println("Headers received: ${headers.keys}")
        println("x-forwarded-for: ${headers.getFirst(HEADER_FORWARDED_FOR)}")
        println("x-rptid: ${headers.getFirst(HEADER_RPT_ID)}")

        // extract header values
        val clientIp =
            headers.getFirst(HEADER_FORWARDED_FOR)
                ?: AuthenticationTracingUtils.TracingEntry.CLIENT_IP.defaultValue
        val rptId =
            headers.getFirst(HEADER_RPT_ID)
                ?: AuthenticationTracingUtils.TracingEntry.RPT_ID.defaultValue

        // create info
        val authenticationInfo =
            AuthenticationTracingUtils.AuthenticationInfo(
                clientIp = clientIp,
                rptId = rptId,
                requestMethod = method,
                requestUriPath = path,
            )

        // add the extracted values to the Reactor context
        return chain.filter(exchange).contextWrite { context ->
            val utils = AuthenticationTracingUtils()
            utils.setAuthInfoIntoReactorContext(authenticationInfo, context)
        }
    }
}
