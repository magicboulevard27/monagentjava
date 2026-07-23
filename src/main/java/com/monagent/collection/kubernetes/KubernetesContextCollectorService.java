package com.monagent.collection.kubernetes;

import com.monagent.collection.SignalNormalizationService;
import com.monagent.collection.model.KubernetesSourceSignal;
import com.monagent.collection.model.NormalizedSignal;
import com.monagent.domain.MonitoredService;
import com.monagent.persistence.IncidentEvidenceEntity;
import com.monagent.persistence.IncidentEvidenceRepository;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class KubernetesContextCollectorService {

    private static final Logger log = LoggerFactory.getLogger(KubernetesContextCollectorService.class);

    private final KubernetesCollectorProperties properties;
    private final KubernetesEventClient client;
    private final KubernetesRedactor redactor;
    private final KubernetesEventDetector detector;
    private final SignalNormalizationService normalizationService;
    private final IncidentEvidenceRepository incidentEvidenceRepository;

    public KubernetesContextCollectorService(
            KubernetesCollectorProperties properties,
            KubernetesEventClient client,
            KubernetesRedactor redactor,
            KubernetesEventDetector detector,
            SignalNormalizationService normalizationService,
            IncidentEvidenceRepository incidentEvidenceRepository) {
        this.properties = properties;
        this.client = client;
        this.redactor = redactor;
        this.detector = detector;
        this.normalizationService = normalizationService;
        this.incidentEvidenceRepository = incidentEvidenceRepository;
    }

    @Scheduled(fixedDelayString = "${monagent.collectors.kubernetes.interval-seconds:60}000")
    public void collect() {
        // Scheduling hook; service-driven iteration is added when Kubernetes integration is connected.
    }

    public NormalizedSignal collect(MonitoredService service) {
        Map<String, Object> response = client.query(properties.endpoint(), service.kubernetesNamespace(), service.kubernetesWorkloadName(), properties.timeout());
        String message = redactor.redact(stringify(response));
        String eventType = detector.detect(stringify(response.get("eventType")), message);
        String resourceKind = stringify(response.getOrDefault("resourceKind", "Pod"));

        KubernetesSourceSignal source = new KubernetesSourceSignal(
                service.serviceId(),
                service.serviceName(),
                service.environment(),
                Instant.now(),
                resourceKind,
                eventType,
                message,
                stringify(response));

        NormalizedSignal normalized = normalizationService.fromKubernetes(source);
        persistEvidence(service, normalized, resourceKind, eventType, message, source);
        return normalized;
    }

    private void persistEvidence(MonitoredService service, NormalizedSignal signal, String resourceKind, String eventType, String message, KubernetesSourceSignal source) {
        IncidentEvidenceEntity entity = new IncidentEvidenceEntity();
        entity.setEvidenceId(UUID.randomUUID());
        entity.setIncidentId(UUID.randomUUID());
        entity.setSourceType("KUBERNETES");
        entity.setServiceName(service.serviceName());
        entity.setEvidenceType(resourceKind + ":" + eventType);
        entity.setDescription(message);
        entity.setObservedAt(source.observedAt());
        entity.setReferenceId(source.rawReference());
        entity.setRedactedPayload(message);
        incidentEvidenceRepository.saveAndFlush(entity);
    }

    private String stringify(Object value) {
        return value == null ? "" : value.toString();
    }
}
