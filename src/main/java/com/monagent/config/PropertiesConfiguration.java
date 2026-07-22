package com.monagent.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({MonitoringProperties.class, RuntimeProperties.class})
public class PropertiesConfiguration {
}
