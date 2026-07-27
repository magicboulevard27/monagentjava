package com.monagent.collection.traces;

import com.monagent.collection.SignalNormalizationService;
import com.monagent.collection.model.NormalizedSignal;
import com.monagent.collection.model.TraceSourceSignal;
import com.monagent.domain.MonitoredService;
import com.monagent.persistence.IncidentEvidenceEntity;
import com.monagent.persistence.IncidentEvidenceRepository;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class TraceAnalyzerService {

    private static final Logger log = LoggerFactory.getLogger(TraceAnalyzerService.class);

    private final TraceAnalyzerProperties properties;
    private final TraceQueryClient client;
    private final TraceRedactor redactor;
    private final TracePatternDetector detector;
    private final SignalNormalizationService normalizationService;
    private final IncidentEvidenceRepository incidentEvidenceRepository;

    public TraceAnalyzerService(
            TraceAnalyzerProperties properties,
            TraceQueryClient client,
            TraceRedactor redactor,
            TracePatternDetector detector,
            SignalNormalizationService normalizationService,
            IncidentEvidenceRepository incidentEvidenceRepository) {
        this.properties = properties;
        this.client = client;
        this.redactor = redactor;
        this.detector = detector;
        this.normalizationService = normalizationService;
        this.incidentEvidenceRepository = incidentEvidenceRepository;
    }

    @Scheduled(fixedDelayString = "${monagent.collectors.traces.interval-seconds:60}000")
    public void analyze() {
        // Wiring placeholder; service-specific iteration is added when trace sources are connected.
    }

    public NormalizedSignal analyze(MonitoredService service, String operation, String status) {
        Instant end = Instant.now();
        Instant start = end.minus(properties.windowMinutes(), ChronoUnit.MINUTES);
        Map<String, Object> response = client.query(new TraceQuery(
                service.serviceName(),
                operation,
                status,
                start,
                end,
                properties.timeout())).payload();
        long durationMillis = extractDuration(response);
        String summary = redactor.redact(stringify(response));
        String dependencyName = extractDependencyName(response);
        String spanName = extractSpanName(response);
        String pattern = detector.detect(summary, durationMillis, status, dependencyName);

        TraceSourceSignal source = new TraceSourceSignal(
                service.serviceId(),
                service.serviceName(),
                service.environment(),
                Instant.now(),
                spanName,
                durationMillis,
                status,
                dependencyName,
                stringify(response));
        NormalizedSignal normalized = normalizationService.fromTrace(source);
        persistEvidence(service, pattern, summary, source);
        return normalized;
    }

    private void persistEvidence(MonitoredService service, String pattern, String summary, TraceSourceSignal source) {
        IncidentEvidenceEntity entity = new IncidentEvidenceEntity();
        entity.setEvidenceId(UUID.randomUUID());
        entity.setIncidentId(UUID.randomUUID());
        entity.setSourceType("TRACES");
        entity.setServiceName(service.serviceName());
        entity.setEvidenceType(pattern);
        entity.setDescription(summary);
        entity.setObservedAt(source.observedAt());
        entity.setReferenceId(source.rawReference());
        entity.setRedactedPayload(summary);
        incidentEvidenceRepository.saveAndFlush(entity);
    }

    private long extractDuration(Map<String, Object> response) {
        Object data = response.get("data");
        if (data instanceof Map<?, ?> dataMap) {
            Object duration = dataMap.get("durationMillis");
            if (duration != null) {
                try {
                    return Long.parseLong(duration.toString());
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return 0L;
    }

    private String extractSpanName(Map<String, Object> response) {
        Object span = response.get("spanName");
        return span == null ? "trace.span" : span.toString();
    }

    private String extractDependencyName(Map<String, Object> response) {
        Object dependency = response.get("dependencyName");
        return dependency == null ? "" : dependency.toString();
    }

    private String stringify(Object value) {
        return value == null ? "" : value.toString();
    }
}
