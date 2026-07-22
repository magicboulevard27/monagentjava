package com.monagent.config;

import com.monagent.collection.kubernetes.KubernetesCollectorProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(KubernetesCollectorProperties.class)
public class KubernetesCollectorConfiguration {
}
