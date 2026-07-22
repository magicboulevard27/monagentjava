package com.monagent.config;

import com.monagent.collection.traces.TraceAnalyzerProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(TraceAnalyzerProperties.class)
public class TraceAnalyzerConfiguration {
}
