package com.monagent.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Table(name = "monitored_services")
public class MonitoredServiceEntity {

    @Id
    @Column(name = "service_id", nullable = false, updatable = false)
    private UUID serviceId;

    @Column(name = "service_name", nullable = false, length = 200)
    private String serviceName;

    @Column(name = "environment", nullable = false, length = 50)
    private String environment;

    @Column(name = "owner_team", nullable = false, length = 200)
    private String ownerTeam;

    @Column(name = "health_url", nullable = false, length = 500)
    private String healthUrl;

    @Column(name = "prometheus_job_name", length = 200)
    private String prometheusJobName;

    @Column(name = "log_index_pattern", length = 200)
    private String logIndexPattern;

    @Column(name = "trace_service_name", length = 200)
    private String traceServiceName;

    @Column(name = "kubernetes_namespace", length = 200)
    private String kubernetesNamespace;

    @Column(name = "kubernetes_workload_name", length = 200)
    private String kubernetesWorkloadName;

    @JdbcTypeCode(SqlTypes.LONG32VARCHAR)
    @Column(name = "alert_channels", nullable = false)
    private String alertChannels;

    @Column(name = "enabled", nullable = false)
    private boolean enabled = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public MonitoredServiceEntity() {
    }

    public MonitoredServiceEntity(UUID serviceId) {
        this.serviceId = serviceId;
    }

    public UUID getServiceId() { return serviceId; }
    public String getServiceName() { return serviceName; }
    public void setServiceName(String serviceName) { this.serviceName = serviceName; }
    public String getEnvironment() { return environment; }
    public void setEnvironment(String environment) { this.environment = environment; }
    public String getOwnerTeam() { return ownerTeam; }
    public void setOwnerTeam(String ownerTeam) { this.ownerTeam = ownerTeam; }
    public String getHealthUrl() { return healthUrl; }
    public void setHealthUrl(String healthUrl) { this.healthUrl = healthUrl; }
    public String getPrometheusJobName() { return prometheusJobName; }
    public void setPrometheusJobName(String prometheusJobName) { this.prometheusJobName = prometheusJobName; }
    public String getLogIndexPattern() { return logIndexPattern; }
    public void setLogIndexPattern(String logIndexPattern) { this.logIndexPattern = logIndexPattern; }
    public String getTraceServiceName() { return traceServiceName; }
    public void setTraceServiceName(String traceServiceName) { this.traceServiceName = traceServiceName; }
    public String getKubernetesNamespace() { return kubernetesNamespace; }
    public void setKubernetesNamespace(String kubernetesNamespace) { this.kubernetesNamespace = kubernetesNamespace; }
    public String getKubernetesWorkloadName() { return kubernetesWorkloadName; }
    public void setKubernetesWorkloadName(String kubernetesWorkloadName) { this.kubernetesWorkloadName = kubernetesWorkloadName; }
    public String getAlertChannels() { return alertChannels; }
    public void setAlertChannels(String alertChannels) { this.alertChannels = alertChannels; }
    public boolean isEnabled() { return enabled; }
    public void setEnabled(boolean enabled) { this.enabled = enabled; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
