package com.monagent.config;

import com.monagent.collection.prometheus.PrometheusCollectorProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(PrometheusCollectorProperties.class)
public class PrometheusCollectorConfiguration {
}
