package com.monagent.config;

import java.time.Duration;
import io.netty.channel.ChannelOption;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class HttpClientConfiguration {

    @Bean
    WebClient webClient(WebClient.Builder builder) {
        return builder.build();
    }

    @Bean
    WebClient.Builder webClientBuilder() {
        ConnectionProvider provider = ConnectionProvider.builder("monagent-webclient")
                .maxConnections(100)
                .pendingAcquireTimeout(Duration.ofSeconds(5))
                .pendingAcquireMaxCount(200)
                .maxIdleTime(Duration.ofSeconds(30))
                .maxLifeTime(Duration.ofMinutes(5))
                .build();
        HttpClient httpClient = HttpClient.create(provider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 3000)
                .responseTimeout(Duration.ofSeconds(10));
        return WebClient.builder().clientConnector(new ReactorClientHttpConnector(httpClient));
    }
}
