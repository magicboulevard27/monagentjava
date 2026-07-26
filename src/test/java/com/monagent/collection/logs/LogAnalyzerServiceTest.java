package com.monagent.collection.logs;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.monagent.collection.SignalNormalizationService;
import com.monagent.domain.MonitoredService;
import com.monagent.persistence.IncidentEvidenceRepository;
import java.net.URI;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class LogAnalyzerServiceTest {

    private final LogAnalyzerProperties properties = new LogAnalyzerProperties(
            URI.create("https://opensearch.internal:9200"),
            "/_search",
            Duration.ofSeconds(5),
            15,
            3,
            50,
            2,
            null,
            null,
            null);
    private final LogSearchClient client = mock(LogSearchClient.class);
    private final LogRedactor redactor = new LogRedactor();
    private final LogPatternDetector detector = new LogPatternDetector();
    private final SignalNormalizationService normalizationService = new SignalNormalizationService();
    private final IncidentEvidenceRepository repository = mock(IncidentEvidenceRepository.class);
    private final LogAnalyzerService service = new LogAnalyzerService(properties, client, redactor, detector, normalizationService, repository);

    @Test
    void groupsRepeatedLogEventsAndCountsOccurrences() {
        Map<String, Object> response = Map.of(
                "hits", Map.of(
                        "hits", List.of(
                                Map.of("_source", Map.of("message", "database connection timeout while querying orders")),
                                Map.of("_source", Map.of("message", "database connection timeout while querying payments")),
                                Map.of("_source", Map.of("message", "java.lang.OutOfMemoryError: Java heap space")))));
        List<LogFinding> findings = service.groupRepeatedEvents(response);

        assertThat(findings).hasSize(2);
        assertThat(findings).anySatisfy(finding -> {
            assertThat(finding.pattern()).isEqualTo("timeout");
            assertThat(finding.occurrenceCount()).isEqualTo(2L);
        });
        assertThat(findings).anySatisfy(finding -> {
            assertThat(finding.pattern()).isEqualTo("outofmemoryerror");
            assertThat(finding.occurrenceCount()).isEqualTo(1L);
        });
    }

    @Test
    void analyzesRepeatedLogsAndPersistsGroupedEvidence() {
        MonitoredService monitoredService = new MonitoredService(
                UUID.randomUUID(),
                "orders",
                "prod",
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                true);
        when(client.query(properties.endpoint(), "orders", "prod", "error", properties.timeout())).thenReturn(Map.of(
                "hits", Map.of("hits", List.of(
                        Map.of("_source", Map.of("message", "database connection timeout")),
                        Map.of("_source", Map.of("message", "database connection timeout"))))));

        var signal = service.analyze(monitoredService, "error");

        assertThat(signal.signalName()).isEqualTo("timeout");
        assertThat(signal.signalValue()).contains("database connection timeout");
    }

    @Test
    void extractsTimestampCorrelationIdAndExceptionType() {
        Map<String, Object> response = Map.of(
                "hits", Map.of("hits", List.of(Map.of(
                        "_source", Map.of(
                                "@timestamp", "2026-07-24T12:34:56Z",
                                "message", "correlationId=abc-123 java.lang.IllegalStateException: boom")))));

        List<LogFinding> findings = service.groupRepeatedEvents(response);

        assertThat(findings).hasSize(1);
        LogFinding finding = findings.get(0);
        assertThat(finding.timestamp()).isEqualTo(Instant.parse("2026-07-24T12:34:56Z"));
        assertThat(finding.correlationId()).isEqualTo("abc-123");
        assertThat(finding.exceptionType()).isEqualTo("java.lang.IllegalStateException");
        assertThat(finding.summary()).contains("correlationId=abc-123");
    }
}
