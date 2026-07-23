package com.monagent.collection.logs;

import com.monagent.collection.SignalNormalizationService;
import com.monagent.collection.model.LogSourceSignal;
import com.monagent.collection.model.NormalizedSignal;
import com.monagent.domain.MonitoredService;
import com.monagent.persistence.IncidentEvidenceEntity;
import com.monagent.persistence.IncidentEvidenceRepository;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class LogAnalyzerService {

    private static final Logger log = LoggerFactory.getLogger(LogAnalyzerService.class);

    private final LogAnalyzerProperties properties;
    private final LogSearchClient client;
    private final LogRedactor redactor;
    private final LogPatternDetector detector;
    private final SignalNormalizationService normalizationService;
    private final IncidentEvidenceRepository incidentEvidenceRepository;

    public LogAnalyzerService(
            LogAnalyzerProperties properties,
            LogSearchClient client,
            LogRedactor redactor,
            LogPatternDetector detector,
            SignalNormalizationService normalizationService,
            IncidentEvidenceRepository incidentEvidenceRepository) {
        this.properties = properties;
        this.client = client;
        this.redactor = redactor;
        this.detector = detector;
        this.normalizationService = normalizationService;
        this.incidentEvidenceRepository = incidentEvidenceRepository;
    }

    @Scheduled(fixedDelayString = "${monagent.collectors.logs.interval-seconds:60}000")
    public void analyze() {
        // Collection scheduling is wired; service-targeted orchestration comes later.
    }

    public NormalizedSignal analyze(MonitoredService service, String severity) {
        Map<String, Object> response = client.query(properties.endpoint(), service.serviceName(), service.environment(), severity, properties.timeout());
        String message = redactedSummary(response);
        String pattern = detector.detect(message);
        LogSourceSignal source = new LogSourceSignal(
                service.serviceId(),
                service.serviceName(),
                service.environment(),
                Instant.now(),
                pattern,
                message,
                stringify(response));
        NormalizedSignal normalized = normalizationService.fromLog(source);
        persistEvidence(service, normalized, pattern, message);
        return normalized;
    }

    private void persistEvidence(MonitoredService service, NormalizedSignal signal, String pattern, String message) {
        IncidentEvidenceEntity entity = new IncidentEvidenceEntity();
        entity.setEvidenceId(UUID.randomUUID());
        entity.setIncidentId(UUID.randomUUID());
        entity.setSourceType("LOGS");
        entity.setServiceName(service.serviceName());
        entity.setEvidenceType(pattern);
        entity.setDescription(message);
        entity.setObservedAt(signal.collectedAt());
        entity.setReferenceId(signal.rawReference());
        entity.setRedactedPayload(message);
        incidentEvidenceRepository.saveAndFlush(entity);
    }

    private String redactedSummary(Map<String, Object> response) {
        return redactor.redact(stringify(response));
    }

    private String stringify(Object value) {
        return value == null ? "" : value.toString();
    }
}
