package it.pagopa.checkout.authservice.v3.configs

import io.netty.channel.ChannelOption
import io.netty.handler.timeout.ReadTimeoutHandler
import it.pagopa.generated.checkout.oneidentity.ApiClient as OneIdentityApiClient
import it.pagopa.generated.checkout.oneidentity.api.TokenServerApisApi
import it.pagopa.generated.checkout.oneidentity.model.GetJwkSet200ResponseDto
import it.pagopa.generated.checkout.oneidentity.model.TokenDataDto
import java.util.concurrent.TimeUnit
import org.springframework.aot.hint.annotation.RegisterReflectionForBinding
import org.springframework.beans.factory.annotation.Qualifier
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.reactive.ReactorClientHttpConnector
import org.springframework.web.reactive.function.client.WebClient
import reactor.netty.Connection
import reactor.netty.http.client.HttpClient
import reactor.netty.transport.NameResolverProvider.NameResolverSpec

@Configuration("WebClientsConfigv3")
class WebClientsConfig {

    @Bean("webClientv3")
    fun webClient(
        @Value("\${one-identity.server.uri}") serverUri: String,
        @Value("\${one-identity.server.readTimeoutMillis}") readTimeoutMillis: Int,
        @Value("\${one-identity.server.connectionTimeoutMillis}") connectionTimeoutMillis: Int,
    ): WebClient {

        val httpClient =
            HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectionTimeoutMillis)
                .doOnConnected { connection: Connection ->
                    connection.addHandlerLast(
                        ReadTimeoutHandler(readTimeoutMillis.toLong(), TimeUnit.MILLISECONDS)
                    )
                }
                .resolver { nameResolverSpec: NameResolverSpec -> nameResolverSpec.ndots(1) }

        return OneIdentityApiClient.buildWebClientBuilder()
            .clientConnector(ReactorClientHttpConnector(httpClient))
            .baseUrl(serverUri)
            .build()
    }

    @Bean("oneIdentityWebClientv3")
    @RegisterReflectionForBinding(TokenDataDto::class, GetJwkSet200ResponseDto::class)
    fun oneIdentityWebClient(
        @Qualifier("webClientv3") webClient: WebClient,
        @Value("\${one-identity.server.uri}") serverUri: String,
    ): TokenServerApisApi {
        val apiClient = OneIdentityApiClient(webClient).setBasePath(serverUri)

        return TokenServerApisApi(apiClient)
    }
}
