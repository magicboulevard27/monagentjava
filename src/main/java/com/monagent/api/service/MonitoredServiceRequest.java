package com.monagent.api.service;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import java.util.List;

public record MonitoredServiceRequest(
        @NotBlank String serviceName,
        @NotBlank String environment,
        @NotBlank String ownerTeam,
        @NotBlank String healthUrl,
        String prometheusJobName,
        String logIndexPattern,
        String traceServiceName,
        String kubernetesNamespace,
        String kubernetesWorkloadName,
        @NotEmpty List<@NotBlank String> alertChannels,
        boolean enabled) {
}
