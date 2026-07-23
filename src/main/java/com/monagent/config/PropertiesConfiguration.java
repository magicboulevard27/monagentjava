package com.monagent.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        MonitoringProperties.class,
        RuntimeProperties.class,
        IntegrationProperties.class,
        ReloadableConfigurationProperties.class,
        RetentionProperties.class
})
public class PropertiesConfiguration {
}
