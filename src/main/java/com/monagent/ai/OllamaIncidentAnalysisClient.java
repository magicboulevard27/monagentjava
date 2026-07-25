package com.monagent.ai;

import com.monagent.config.IntegrationProperties;
import io.github.resilience4j.bulkhead.Bulkhead;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import java.time.Duration;
import java.util.function.Supplier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class OllamaIncidentAnalysisClient implements IncidentAnalysisClient {

    private final WebClient webClient;
    private final IntegrationProperties.Ollama ollamaProperties;
    private final Retry retry;
    private final CircuitBreaker circuitBreaker;
    private final Bulkhead bulkhead;

    public OllamaIncidentAnalysisClient(WebClient.Builder webClientBuilder,
                                        IntegrationProperties integrationProperties) {
        this.ollamaProperties = integrationProperties.ollama();
        this.webClient = webClientBuilder
                .baseUrl(this.ollamaProperties.baseUrl().toString())
                .build();
        this.retry = Retry.of("ollamaIncidentAnalysis", RetryConfig.custom()
                .maxAttempts(Math.max(1, this.ollamaProperties.retryAttempts()))
                .waitDuration(Duration.ofMillis(200))
                .retryExceptions(RuntimeException.class)
                .build());
        this.circuitBreaker = CircuitBreaker.of("ollamaIncidentAnalysis", CircuitBreakerConfig.custom()
                .failureRateThreshold(Math.max(1, Math.min(100, this.ollamaProperties.circuitBreakerFailureThreshold())))
                .minimumNumberOfCalls(2)
                .slidingWindowSize(4)
                .waitDurationInOpenState(Duration.ofSeconds(5))
                .build());
        this.bulkhead = Bulkhead.of("ollamaIncidentAnalysis", BulkheadConfig.custom()
                .maxConcurrentCalls(Math.max(1, this.ollamaProperties.bulkheadMaxConcurrentCalls()))
                .maxWaitDuration(Duration.ZERO)
                .build());
    }

    @Override
    public String analyze(String prompt) {
        Supplier<String> supplier = () -> webClient.post()
                .uri("/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(new OllamaRequest(ollamaProperties.model(), prompt, false))
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(ollamaProperties.requestTimeoutSeconds()))
                .block();
        Supplier<String> decorated = Bulkhead.decorateSupplier(bulkhead,
                CircuitBreaker.decorateSupplier(circuitBreaker, Retry.decorateSupplier(retry, supplier)));
        return decorated.get();
    }

    private record OllamaRequest(String model, String prompt, boolean stream) {
    }
}
