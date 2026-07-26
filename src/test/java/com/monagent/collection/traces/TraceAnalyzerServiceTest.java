package com.monagent.collection.traces;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.when;

import com.monagent.collection.SignalNormalizationService;
import com.monagent.domain.MonitoredService;
import com.monagent.persistence.IncidentEvidenceRepository;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TraceAnalyzerServiceTest {

    private final TraceAnalyzerProperties properties = new TraceAnalyzerProperties(
            "https://jaeger.internal/api/traces",
            Duration.ofSeconds(5),
            15,
            3,
            50,
            2);
    private final TraceQueryClient client = mock(TraceQueryClient.class);
    private final TraceRedactor redactor = new TraceRedactor();
    private final TracePatternDetector detector = new TracePatternDetector();
    private final SignalNormalizationService normalizationService = new SignalNormalizationService();
    private final IncidentEvidenceRepository repository = mock(IncidentEvidenceRepository.class);
    private final TraceAnalyzerService service = new TraceAnalyzerService(
            properties,
            client,
            redactor,
            detector,
            normalizationService,
            repository);

    @Test
    void analyzesTraceThroughNeutralQueryInterface() {
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
                List.of(),
                true);
        when(client.query(any(TraceQuery.class)))
                .thenReturn(new TraceQueryResult(Map.of(
                        "data", Map.of("durationMillis", 2501),
                        "spanName", "GET /checkout",
                        "dependencyName", "payments")));

        var signal = service.analyze(monitoredService, "GET /checkout", "error");

        assertThat(signal.signalName()).isEqualTo("get./checkout");
        assertThat(signal.signalValue()).isEqualTo("2501");
    }
}
