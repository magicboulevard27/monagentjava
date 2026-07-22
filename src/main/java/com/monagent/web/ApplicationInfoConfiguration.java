package com.monagent.web;

import com.monagent.config.MonitoringProperties;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.boot.actuate.info.Info;
import org.springframework.boot.actuate.info.InfoContributor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ApplicationInfoConfiguration {

    @Bean
    Clock clock() {
        return Clock.systemUTC();
    }

    @Bean
    InfoContributor applicationInfoContributor(MonitoringProperties monitoringProperties, Clock clock) {
        return builder -> {
            Map<String, Object> details = new LinkedHashMap<>();
            details.put("application", monitoringProperties.applicationName());
            details.put("baseline", "Java 21 / Spring Boot 3.x");
            details.put("timestamp", Instant.now(clock).toString());
            builder.withDetail("application", details);
        };
    }
}
