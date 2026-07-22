package com.monagent.ai;

import com.monagent.config.IntegrationProperties;
import java.time.Duration;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class OllamaIncidentAnalysisClient implements IncidentAnalysisClient {

    private final WebClient webClient;
    private final IntegrationProperties.Ollama ollamaProperties;

    public OllamaIncidentAnalysisClient(WebClient.Builder webClientBuilder,
                                        IntegrationProperties integrationProperties) {
        this.ollamaProperties = integrationProperties.ollama();
        this.webClient = webClientBuilder
                .baseUrl(this.ollamaProperties.baseUrl().toString())
                .build();
    }

    @Override
    public String analyze(String prompt) {
        OllamaRequest request = new OllamaRequest(ollamaProperties.model(), prompt, false);
        return webClient.post()
                .uri("/api/generate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(request)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(ollamaProperties.requestTimeoutSeconds()))
                .block();
    }

    private record OllamaRequest(String model, String prompt, boolean stream) {
    }
}
