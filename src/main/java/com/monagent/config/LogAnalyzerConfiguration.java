package com.monagent.config;

import com.monagent.collection.logs.LogAnalyzerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(LogAnalyzerProperties.class)
public class LogAnalyzerConfiguration {
}
