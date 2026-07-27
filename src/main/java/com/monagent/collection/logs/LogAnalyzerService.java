package com.monagent.collection.logs;

import com.monagent.collection.SignalNormalizationService;
import com.monagent.collection.model.LogSourceSignal;
import com.monagent.collection.model.NormalizedSignal;
import com.monagent.domain.MonitoredService;
import com.monagent.persistence.IncidentEvidenceEntity;
import com.monagent.persistence.IncidentEvidenceRepository;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class LogAnalyzerService {

    private static final Logger log = LoggerFactory.getLogger(LogAnalyzerService.class);
    private static final Pattern CORRELATION_ID_PATTERN = Pattern.compile("\\b(?:correlation[-_ ]?id|trace[-_ ]?id|request[-_ ]?id)[:=\\s]+([A-Za-z0-9\\-]+)\\b", Pattern.CASE_INSENSITIVE);
    private static final Pattern EXCEPTION_PATTERN = Pattern.compile("\\b([A-Za-z0-9_$.]+(?:Exception|Error))\\b");

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
        log.debug("Scheduled log analysis tick started");
    }

    public NormalizedSignal analyze(MonitoredService service, String severity) {
        log.info("Analyzing logs serviceName={} environment={} severity={}", service.serviceName(), service.environment(), severity);
        Map<String, Object> response = client.query(properties.endpoint(), service.serviceName(), service.environment(), severity, properties.timeout());
        List<LogFinding> findings = groupRepeatedEvents(response);
        LogFinding primaryFinding = findings.isEmpty() ? fallbackFinding(service, response) : findings.get(0);
        String message = redactedSummary(response);
        String pattern = primaryFinding.pattern();
        LogSourceSignal source = new LogSourceSignal(
                service.serviceId(),
                service.serviceName(),
                service.environment(),
                primaryFinding.timestamp(),
                pattern,
                primaryFinding.summary(),
                stringify(response));
        NormalizedSignal normalized = normalizationService.fromLog(source);
        persistEvidence(service, normalized, primaryFinding);
        log.info("Log analysis completed serviceName={} pattern={} occurrences={} signalId={}",
                service.serviceName(), pattern, primaryFinding.occurrenceCount(), normalized.signalId());
        return normalized;
    }

    List<LogFinding> groupRepeatedEvents(Map<String, Object> response) {
        List<?> hits = extractHits(response);
        Map<String, Long> counts = new LinkedHashMap<>();
        Map<String, String> summaries = new LinkedHashMap<>();
        for (Object hit : hits) {
            String summary = summarizeHit(hit);
            String pattern = detector.detect(summary);
            counts.merge(pattern, 1L, Long::sum);
            summaries.putIfAbsent(pattern, summary);
        }
        if (counts.isEmpty()) {
            return List.of();
        }
        List<LogFinding> findings = new ArrayList<>();
        for (Map.Entry<String, Long> entry : counts.entrySet()) {
            String summary = summaries.getOrDefault(entry.getKey(), "");
            findings.add(new LogFinding(null, null, null, entry.getKey(), summary,
                    entry.getValue(), extractTimestamp(summary, response), extractCorrelationId(summary),
                    extractExceptionType(summary), Instant.now(), stringify(response)));
        }
        return findings;
    }

    private LogFinding fallbackFinding(MonitoredService service, Map<String, Object> response) {
        String summary = redactedSummary(response);
        return new LogFinding(service.serviceId(), service.serviceName(), service.environment(), detector.detect(summary),
                safeSummary(summary), 1L, extractTimestamp(summary, response), extractCorrelationId(summary),
                extractExceptionType(summary), Instant.now(), stringify(response));
    }

    private void persistEvidence(MonitoredService service, NormalizedSignal signal, LogFinding finding) {
        IncidentEvidenceEntity entity = new IncidentEvidenceEntity();
        entity.setEvidenceId(UUID.randomUUID());
        entity.setIncidentId(UUID.randomUUID());
        entity.setSourceType("LOGS");
        entity.setServiceName(service.serviceName());
        entity.setEvidenceType(finding.pattern());
        entity.setDescription(finding.summary() + " occurrences=" + finding.occurrenceCount());
        entity.setObservedAt(signal.collectedAt());
        entity.setReferenceId(signal.rawReference());
        entity.setRedactedPayload(finding.summary() + " occurrences=" + finding.occurrenceCount());
        incidentEvidenceRepository.saveAndFlush(entity);
        log.debug("Persisted log evidence serviceName={} signalId={} pattern={}", service.serviceName(), signal.signalId(), finding.pattern());
    }

    private String redactedSummary(Map<String, Object> response) {
        return redactor.redact(stringify(response));
    }

    private List<?> extractHits(Map<String, Object> response) {
        if (response == null) {
            return List.of();
        }
        Object hits = response.get("hits");
        if (hits instanceof Map<?, ?> hitMap) {
            Object inner = hitMap.get("hits");
            if (inner instanceof List<?> list) {
                return list;
            }
        }
        if (hits instanceof List<?> list) {
            return list;
        }
        return List.of();
    }

    private String summarizeHit(Object hit) {
        if (!(hit instanceof Map<?, ?> map)) {
            return stringify(hit);
        }
        String message = null;
        String timestamp = null;
        Object source = map.get("_source");
        if (source instanceof Map<?, ?> sourceMap) {
            Object messageValue = sourceMap.get("message");
            if (messageValue != null) {
                message = stringify(messageValue);
            }
            Object timestampValue = sourceMap.get("@timestamp");
            if (timestampValue != null) {
                timestamp = stringify(timestampValue);
            }
        }
        if (message == null) {
            Object messageValue = map.get("message");
            message = messageValue == null ? stringify(map) : stringify(messageValue);
        }
        if (timestamp == null) {
            Object timestampValue = map.get("@timestamp");
            if (timestampValue != null) {
                timestamp = stringify(timestampValue);
            }
        }
        return timestamp == null ? message : timestamp + " " + message;
    }

    private String safeSummary(String summary) {
        if (summary == null) {
            return "";
        }
        return summary.length() > 240 ? summary.substring(0, 240) : summary;
    }

    private Instant extractTimestamp(String summary, Map<String, Object> response) {
        String candidate = extractField(summary, response, "@timestamp");
        if (candidate == null || candidate.isBlank()) {
            return Instant.now();
        }
        try {
            return Instant.parse(candidate);
        } catch (DateTimeParseException ignored) {
            try {
                return OffsetDateTime.parse(candidate).toInstant();
            } catch (DateTimeParseException ignoredToo) {
                return Instant.now();
            }
        }
    }

    private String extractCorrelationId(String summary) {
        Matcher matcher = CORRELATION_ID_PATTERN.matcher(summary == null ? "" : summary);
        return matcher.find() ? matcher.group(1) : "";
    }

    private String extractExceptionType(String summary) {
        Matcher matcher = EXCEPTION_PATTERN.matcher(summary == null ? "" : summary);
        return matcher.find() ? matcher.group(1) : "";
    }

    private String extractField(String summary, Map<String, Object> response, String fieldName) {
        if (summary != null && summary.contains(fieldName)) {
            int index = summary.indexOf(fieldName);
            int separator = summary.indexOf('=', index);
            if (separator < 0) {
                separator = summary.indexOf(':', index);
            }
            if (separator > 0) {
                int end = summary.indexOf(' ', separator + 1);
                return end > separator ? summary.substring(separator + 1, end).trim() : summary.substring(separator + 1).trim();
            }
        }
        if (response == null) {
            return null;
        }
        Object hits = response.get("hits");
        if (hits instanceof Map<?, ?> hitMap) {
            Object inner = hitMap.get("hits");
            if (inner instanceof List<?> list && !list.isEmpty() && list.get(0) instanceof Map<?, ?> map) {
                Object source = map.get("_source");
                if (source instanceof Map<?, ?> sourceMap) {
                    Object value = sourceMap.get(fieldName);
                    return value == null ? null : stringify(value);
                }
            }
        }
        return null;
    }

    private String stringify(Object value) {
        return value == null ? "" : value.toString();
    }
}
