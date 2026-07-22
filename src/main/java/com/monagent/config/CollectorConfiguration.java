package com.monagent.config;

import com.monagent.collection.health.HealthCollectorProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(HealthCollectorProperties.class)
public class CollectorConfiguration {
}
