package com.monagent.analysis;

import com.monagent.persistence.IncidentEntity;
import com.monagent.persistence.IncidentEvidenceEntity;
import com.monagent.persistence.IncidentEvidenceRepository;
import com.monagent.persistence.IncidentRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IncidentCorrelationService {

    private static final Logger log = LoggerFactory.getLogger(IncidentCorrelationService.class);

    private static final Duration DEFAULT_CORRELATION_WINDOW = Duration.ofMinutes(30);

    private final IncidentRepository incidentRepository;
    private final IncidentEvidenceRepository incidentEvidenceRepository;

    public IncidentCorrelationService(IncidentRepository incidentRepository,
                                      IncidentEvidenceRepository incidentEvidenceRepository) {
        this.incidentRepository = incidentRepository;
        this.incidentEvidenceRepository = incidentEvidenceRepository;
    }

    @Transactional
    public IncidentCandidate correlate(List<AnomalyOutcome> anomalies) {
        if (anomalies == null || anomalies.isEmpty()) {
            throw new IllegalArgumentException("At least one anomaly outcome is required");
        }

        log.info("Correlating {} anomaly outcomes into an incident", anomalies.size());
        List<AnomalyOutcome> sorted = anomalies.stream()
                .sorted(Comparator.comparing(AnomalyOutcome::detectedAt))
                .toList();

        UUID incidentId = UUID.randomUUID();
        Instant startTime = sorted.getFirst().detectedAt();
        Instant detectedAt = sorted.getLast().detectedAt();
        List<String> affectedServices = sorted.stream()
                .map(AnomalyOutcome::serviceId)
                .distinct()
                .map(UUID::toString)
                .toList();

        boolean serviceDown = sorted.stream().anyMatch(outcome -> "DOWN".equalsIgnoreCase(outcome.outcomeStatus()));
        boolean customerImpact = sorted.stream().anyMatch(outcome -> "CRITICAL".equalsIgnoreCase(outcome.severity())
                || "HIGH".equalsIgnoreCase(outcome.severity()));
        boolean dataLoss = sorted.stream().anyMatch(outcome -> "error.rate".equalsIgnoreCase(outcome.metricName())
                && outcome.observedValue() != null
                && outcome.observedValue().doubleValue() > 10);

        IncidentImpact impact = assessImpact(sorted, customerImpact);
        String severity = IncidentSeverityClassifier.classify(serviceDown, dataLoss, customerImpact, affectedServices.size());
        String title = buildTitle(sorted, severity);
        String summary = buildSummary(sorted);
        String likelyRootCause = inferLikelyRootCause(sorted);
        String confidence = impact.blastRadiusElevated() ? "MEDIUM" : "HIGH";

        IncidentEntity incident = new IncidentEntity();
        incident.setIncidentId(incidentId);
        incident.setTitle(title);
        incident.setSeverity(severity);
        incident.setStatus("ACTIVE");
        incident.setAffectedServices(toJsonArray(affectedServices));
        incident.setStartTime(startTime);
        incident.setDetectedAt(detectedAt);
        incident.setLikelyRootCause(likelyRootCause);
        incident.setConfidence(confidence);
        incident.setSummary(summary);
        incidentRepository.saveAndFlush(incident);
        log.info("Created incident incidentId={} severity={} affectedServices={}", incidentId, severity, affectedServices.size());

        List<IncidentEvidenceEntity> evidenceEntities = new ArrayList<>();
        for (AnomalyOutcome anomaly : sorted) {
            IncidentEvidenceEntity evidence = new IncidentEvidenceEntity();
            evidence.setEvidenceId(UUID.randomUUID());
            evidence.setIncidentId(incidentId);
            evidence.setSourceType("ANOMALY");
            evidence.setServiceName(anomaly.metricName());
            evidence.setEvidenceType(anomaly.comparator().name());
            evidence.setDescription(anomaly.outcomeStatus() + " on " + anomaly.metricName());
            evidence.setObservedAt(anomaly.detectedAt());
            evidence.setReferenceId(anomaly.signalId() == null ? null : anomaly.signalId().toString());
            evidence.setRedactedPayload(toJsonMap(anomaly));
            evidenceEntities.add(evidence);
        }
        incidentEvidenceRepository.saveAllAndFlush(evidenceEntities);
        log.debug("Persisted {} incident evidence records incidentId={}", evidenceEntities.size(), incidentId);

        List<IncidentEvidence> evidence = evidenceEntities.stream()
                .map(entity -> new IncidentEvidence(
                        entity.getEvidenceId(),
                        entity.getIncidentId(),
                        entity.getSourceType(),
                        entity.getServiceName(),
                        entity.getEvidenceType(),
                        entity.getDescription(),
                        entity.getObservedAt(),
                        entity.getReferenceId(),
                        Map.of("payload", entity.getRedactedPayload())))
                .toList();

        return new IncidentCandidate(
                incidentId,
                title,
                severity,
                "ACTIVE",
                affectedServices,
                startTime,
                detectedAt,
                null,
                likelyRootCause,
                confidence,
                summary,
                evidence);
    }

    @Transactional
    public List<IncidentCandidate> mergeDuplicateCandidates(List<IncidentCandidate> candidates) {
        log.info("Merging {} incident candidates", candidates == null ? 0 : candidates.size());
        Map<String, List<IncidentCandidate>> grouped = candidates.stream()
                .collect(Collectors.groupingBy(this::deduplicationKey, LinkedHashMap::new, Collectors.toList()));

        List<IncidentCandidate> merged = new ArrayList<>();
        for (List<IncidentCandidate> group : grouped.values()) {
            merged.add(mergeGroup(group));
        }
        log.info("Merged {} candidate groups into {} candidates", grouped.size(), merged.size());
        return merged;
    }

    @Transactional
    public IncidentCandidate updateIncidentState(UUID incidentId, IncidentLifecycleState nextState, Instant resolvedAt) {
        IncidentEntity incident = incidentRepository.findById(incidentId)
                .orElseThrow(() -> new IllegalArgumentException("Incident not found: " + incidentId));
        incident.setStatus(nextState.name());
        if (resolvedAt != null) {
            incident.setResolvedAt(resolvedAt);
        }
        incidentRepository.saveAndFlush(incident);
        log.info("Updated incident incidentId={} status={} resolvedAt={}", incidentId, nextState, resolvedAt);
        return new IncidentCandidate(
                incident.getIncidentId(),
                incident.getTitle(),
                incident.getSeverity(),
                incident.getStatus(),
                parseAffectedServices(incident.getAffectedServices()),
                incident.getStartTime(),
                incident.getDetectedAt(),
                incident.getResolvedAt(),
                incident.getLikelyRootCause(),
                incident.getConfidence(),
                incident.getSummary(),
                List.of());
    }

    public IncidentLifecycleState transition(IncidentLifecycleState currentState, boolean resolved, boolean suppressed) {
        if (suppressed) {
            return IncidentLifecycleState.SUPPRESSED;
        }
        if (currentState == IncidentLifecycleState.MERGED) {
            return IncidentLifecycleState.MERGED;
        }
        if (resolved) {
            return IncidentLifecycleState.RESOLVED;
        }
        return currentState == null ? IncidentLifecycleState.CANDIDATE : IncidentLifecycleState.ACTIVE;
    }

    public IncidentImpact assessImpact(List<AnomalyOutcome> anomalies, boolean customerImpact) {
        int affectedServiceCount = (int) anomalies.stream()
                .map(AnomalyOutcome::serviceId)
                .distinct()
                .count();
        boolean blastRadiusElevated = customerImpact || affectedServiceCount >= 3;
        return new IncidentImpact(affectedServiceCount, customerImpact, blastRadiusElevated);
    }

    private IncidentCandidate mergeGroup(List<IncidentCandidate> group) {
        List<IncidentCandidate> ordered = group.stream()
                .sorted(Comparator.comparing(IncidentCandidate::detectedAt))
                .toList();
        IncidentCandidate first = ordered.getFirst();
        List<String> affectedServices = ordered.stream()
                .flatMap(candidate -> candidate.affectedServices().stream())
                .distinct()
                .toList();
        List<IncidentEvidence> evidence = ordered.stream()
                .flatMap(candidate -> candidate.evidence().stream())
                .toList();
        return new IncidentCandidate(
                first.incidentId(),
                first.title(),
                first.severity(),
                first.status(),
                affectedServices,
                first.startTime(),
                ordered.getLast().detectedAt(),
                first.resolvedAt(),
                first.likelyRootCause(),
                first.confidence(),
                first.summary(),
                evidence);
    }

    private String deduplicationKey(IncidentCandidate candidate) {
        return String.join("|",
                candidate.status(),
                candidate.severity(),
                candidate.affectedServices().stream().sorted().collect(Collectors.joining(",")),
                String.valueOf(candidate.startTime().truncatedTo(java.time.temporal.ChronoUnit.MINUTES).toEpochMilli()));
    }

    private String buildTitle(List<AnomalyOutcome> anomalies, String severity) {
        String primary = anomalies.getFirst().metricName();
        return severity + " incident on " + primary;
    }

    private String buildSummary(List<AnomalyOutcome> anomalies) {
        return anomalies.stream()
                .map(outcome -> outcome.metricName() + "=" + outcome.observedValue())
                .collect(Collectors.joining(", "));
    }

    private String inferLikelyRootCause(List<AnomalyOutcome> anomalies) {
        return anomalies.stream()
                .map(AnomalyOutcome::metricName)
                .findFirst()
                .map(metric -> "Correlated anomaly in " + metric)
                .orElse("Unknown");
    }

    private String toJsonArray(List<String> values) {
        return values.stream()
                .map(value -> "\"" + value + "\"")
                .collect(Collectors.joining(",", "[", "]"));
    }

    private String toJsonMap(AnomalyOutcome anomaly) {
        return "{\"metricName\":\"" + anomaly.metricName() + "\",\"severity\":\"" + anomaly.severity() + "\"}";
    }

    private List<String> parseAffectedServices(String json) {
        if (json == null || json.isBlank() || "[]".equals(json.trim())) {
            return List.of();
        }
        String trimmed = json.trim();
        if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
            trimmed = trimmed.substring(1, trimmed.length() - 1);
        }
        if (trimmed.isBlank()) {
            return List.of();
        }
        return List.of(trimmed.replace("\"", "").split("\\s*,\\s*"));
    }
}
