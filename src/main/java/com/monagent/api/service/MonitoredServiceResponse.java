package com.monagent.api.service;

import java.util.List;
import java.util.UUID;

public record MonitoredServiceResponse(
        UUID serviceId,
        String serviceName,
        String environment,
        String ownerTeam,
        String healthUrl,
        String prometheusJobName,
        String logIndexPattern,
        String traceServiceName,
        String kubernetesNamespace,
        String kubernetesWorkloadName,
        List<String> alertChannels,
        boolean enabled) {
}
