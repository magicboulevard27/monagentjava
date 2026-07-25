package com.monagent.approval;

import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "monagent.approval")
public record ApprovedActionExecutionProperties(
        @NotNull Boolean allowProductionWriteActions) {
}
