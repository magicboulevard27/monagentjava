package com.monagent.config;

import com.monagent.collection.kubernetes.KubernetesEventClient;
import com.monagent.collection.kubernetes.KubernetesCollectorProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
@EnableConfigurationProperties(KubernetesCollectorProperties.class)
public class KubernetesCollectorConfiguration {

    @Bean
    KubernetesEventClient kubernetesEventClient(WebClient webClient, KubernetesCollectorProperties properties) {
        return new KubernetesEventClient(webClient, properties);
    }
}
