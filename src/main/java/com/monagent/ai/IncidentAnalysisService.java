package com.monagent.ai;

import com.monagent.analysis.AnomalyOutcome;
import com.monagent.analysis.IncidentCandidate;
import com.monagent.analysis.IncidentEvidence;
import com.monagent.analysis.IncidentImpact;
import com.monagent.analysis.IncidentLifecycleState;
import com.monagent.analysis.IncidentSeverityClassifier;
import com.monagent.web.SelfObservabilityMetrics;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class IncidentAnalysisService {

    private final IncidentAnalysisClient client;
    private final IncidentAnalysisPromptBuilder promptBuilder;
    private final IncidentAnalysisResultParser resultParser;
    private final SelfObservabilityMetrics metrics;

    public IncidentAnalysisService(IncidentAnalysisClient client,
                                   IncidentAnalysisPromptBuilder promptBuilder,
                                   IncidentAnalysisResultParser resultParser,
                                   SelfObservabilityMetrics metrics) {
        this.client = client;
        this.promptBuilder = promptBuilder;
        this.resultParser = resultParser;
        this.metrics = metrics;
    }

    public AiAnalysisResult analyze(AiAnalysisRequest request) {
        String prompt = promptBuilder.build(request);
        Instant started = Instant.now();
        try {
            String raw = metrics.time("monagent.ai.analysis.latency", "ollama", () -> client.analyze(prompt));
            Instant finished = Instant.now();
            IncidentAnalysisResultParser.ParsedIncidentAnalysis parsed = resultParser.parse(raw);
            List<String> availableEvidenceIds = request.evidence().stream().map(item -> item.evidenceId().toString()).toList();
            List<String> missingEvidence = resultParser.validateEvidenceReferences(parsed, availableEvidenceIds);
            if (!missingEvidence.isEmpty()) {
                throw new IllegalArgumentException("Analysis referenced missing evidence: " + missingEvidence);
            }
            return new AiAnalysisResult(
                    parsed.incidentId(),
                    parsed.severity(),
                    parsed.affectedServices(),
                    parsed.status(),
                    parsed.symptoms(),
                    parsed.likelyRootCause(),
                    parsed.confidence(),
                    parsed.evidenceIds(),
                    parsed.recommendedActions(),
                    parsed.escalate(),
                    resolveModelName(),
                    IncidentAnalysisPromptBuilder.PROMPT_VERSION,
                    Duration.between(started, finished).toMillis(),
                    estimateTokens(prompt, raw),
                    "SUCCESS");
        } catch (Exception ex) {
            return fallback(request, started, ex);
        }
    }

    private AiAnalysisResult fallback(AiAnalysisRequest request, Instant started, Exception ex) {
        List<String> services = request.incidentCandidate() == null
                ? List.of()
                : request.incidentCandidate().affectedServices();
        String severity = deriveSeverity(request.anomalies());
        List<String> symptoms = request.anomalies().stream()
                .map(AnomalyOutcome::metricName)
                .distinct()
                .toList();
        String rootCause = services.isEmpty() ? "Unknown" : "Correlated anomaly cluster";
        List<String> actions = List.of("Inspect affected services", "Check recent deployments", "Review supporting logs and traces");
        Instant finished = Instant.now();
        return new AiAnalysisResult(
                request.incidentCandidate() == null ? UUID.randomUUID().toString() : request.incidentCandidate().incidentId().toString(),
                severity,
                services,
                IncidentLifecycleState.ACTIVE.name(),
                symptoms,
                rootCause,
                "LOW",
                request.evidence().stream().map(item -> item.evidenceId().toString()).toList(),
                actions,
                "CRITICAL".equals(severity),
                resolveModelName(),
                IncidentAnalysisPromptBuilder.PROMPT_VERSION,
                Duration.between(started, finished).toMillis(),
                0,
                "FALLBACK");
    }

    private String deriveSeverity(List<AnomalyOutcome> anomalies) {
        boolean serviceDown = anomalies.stream().anyMatch(outcome -> "DOWN".equalsIgnoreCase(outcome.outcomeStatus()));
        boolean critical = anomalies.stream().anyMatch(outcome -> "CRITICAL".equalsIgnoreCase(outcome.severity()));
        boolean high = anomalies.stream().anyMatch(outcome -> "HIGH".equalsIgnoreCase(outcome.severity()));
        return IncidentSeverityClassifier.classify(serviceDown, critical, high, (int) anomalies.stream().map(AnomalyOutcome::serviceId).distinct().count());
    }

    private String resolveModelName() {
        return "ollama";
    }

    private int estimateTokens(String prompt, String raw) {
        return Math.max(1, (prompt.length() + (raw == null ? 0 : raw.length())) / 4);
    }
}
