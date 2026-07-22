package com.monagent.api.service;

import com.monagent.domain.MonitoredService;
import com.monagent.persistence.MonitoredServiceEntity;
import java.util.Arrays;
import java.util.List;

final class MonitoredServiceMapper {

    private MonitoredServiceMapper() {
    }

    static MonitoredServiceResponse toResponse(MonitoredService service) {
        return new MonitoredServiceResponse(
                service.serviceId(),
                service.serviceName(),
                service.environment(),
                service.ownerTeam(),
                service.healthUrl(),
                service.prometheusJobName(),
                service.logIndexPattern(),
                service.traceServiceName(),
                service.kubernetesNamespace(),
                service.kubernetesWorkloadName(),
                service.alertChannels(),
                service.enabled());
    }

    static MonitoredService toDomain(MonitoredServiceEntity entity) {
        return new MonitoredService(
                entity.getServiceId(),
                entity.getServiceName(),
                entity.getEnvironment(),
                entity.getOwnerTeam(),
                entity.getHealthUrl(),
                entity.getPrometheusJobName(),
                entity.getLogIndexPattern(),
                entity.getTraceServiceName(),
                entity.getKubernetesNamespace(),
                entity.getKubernetesWorkloadName(),
                splitChannels(entity.getAlertChannels()),
                entity.isEnabled());
    }

    static void applyRequest(MonitoredServiceEntity entity, MonitoredServiceRequest request) {
        entity.setServiceName(request.serviceName());
        entity.setEnvironment(request.environment());
        entity.setOwnerTeam(request.ownerTeam());
        entity.setHealthUrl(request.healthUrl());
        entity.setPrometheusJobName(request.prometheusJobName());
        entity.setLogIndexPattern(request.logIndexPattern());
        entity.setTraceServiceName(request.traceServiceName());
        entity.setKubernetesNamespace(request.kubernetesNamespace());
        entity.setKubernetesWorkloadName(request.kubernetesWorkloadName());
        entity.setAlertChannels(String.join(",", request.alertChannels()));
        entity.setEnabled(request.enabled());
    }

    private static List<String> splitChannels(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(channel -> !channel.isEmpty())
                .toList();
    }
}
