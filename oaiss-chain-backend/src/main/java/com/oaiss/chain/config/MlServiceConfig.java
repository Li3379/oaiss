package com.oaiss.chain.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * ML Service WebClient configuration.
 *
 * <p>Configures a dedicated WebClient bean for calling the Python ML microservice,
 * with connection and read timeouts sourced from application properties.</p>
 *
 * @author OAISS Team
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "ml.service")
public class MlServiceConfig {

    private String url = "http://localhost:8001";
    private Duration connectTimeout = Duration.ofSeconds(5);
    private Duration readTimeout = Duration.ofSeconds(30);

    @Bean
    public WebClient mlWebClient(WebClient.Builder webClientBuilder) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS,
                        (int) connectTimeout.toMillis())
                .responseTimeout(readTimeout)
                .doOnConnected(conn -> conn
                        .addHandlerLast(new ReadTimeoutHandler(readTimeout.toSeconds()))
                        .addHandlerLast(new WriteTimeoutHandler(readTimeout.toSeconds())));

        return webClientBuilder
                .baseUrl(url)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}