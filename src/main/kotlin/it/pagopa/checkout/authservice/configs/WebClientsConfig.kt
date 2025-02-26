package it.pagopa.checkout.authservice.configs

import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import it.pagopa.generated.checkout.oneidentity.ApiClient as OneIdentityApiClient
import it.pagopa.generated.checkout.oneidentity.api.TokenServerApisApi
import java.util.concurrent.TimeUnit
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import reactor.netty.Connection
import reactor.netty.http.client.HttpClient
import reactor.netty.transport.NameResolverProvider.NameResolverSpec

@Configuration
class WebClientsConfig {

    @Bean
    fun oneIdentityWebClient(
        @Value("\${one-identity.server.uri}") serverUri: String,
        @Value("\${one-identity.server.readTimeoutMillis}") readTimeoutMillis: Int,
        @Value("\${one-identity.server.connectionTimeoutMillis}") connectionTimeoutMillis: Int,
    ): TokenServerApisApi {
        val httpClient =
            HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeoutMillis)
                .doOnConnected { connection: Connection ->
                    connection.addHandlerLast(
                        ReadTimeoutHandler(readTimeoutMillis.toLong(), TimeUnit.MILLISECONDS)
                    )
                }
                .resolver { nameResolverSpec: NameResolverSpec -> nameResolverSpec.ndots(1) }

        val webClient =
            OneIdentityApiClient.buildWebClientBuilder()
                .clientConnector(ReactorClientHttpConnector(httpClient))
                .baseUrl(serverUri)
                .build()

        val apiClient = OneIdentityApiClient(webClient).setBasePath(serverUri)
        return TokenServerApisApi(apiClient)
    }
}
