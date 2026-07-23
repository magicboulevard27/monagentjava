package com.monagent.api.service;

import com.monagent.analysis.IncidentCandidate;
import com.monagent.analysis.IncidentCorrelationService;
import com.monagent.analysis.IncidentEvidence;
import com.monagent.analysis.Recommendation;
import com.monagent.analysis.RecommendationEngineService;
import com.monagent.audit.AuditService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class IncidentController {

    private static final Logger log = LoggerFactory.getLogger(IncidentController.class);

    private final IncidentQueryService incidentQueryService;
    private final IncidentCorrelationService incidentCorrelationService;
    private final RecommendationEngineService recommendationEngineService;
    private final AuditService auditService;

    public IncidentController(IncidentQueryService incidentQueryService,
                              IncidentCorrelationService incidentCorrelationService,
                              RecommendationEngineService recommendationEngineService,
                              AuditService auditService) {
        this.incidentQueryService = incidentQueryService;
        this.incidentCorrelationService = incidentCorrelationService;
        this.recommendationEngineService = recommendationEngineService;
        this.auditService = auditService;
    }

    @GetMapping("/incidents")
    public List<IncidentResponse> list(
            @RequestParam(required = false) String severity,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Integer limit,
            @RequestParam(required = false) Integer offset) {
        return incidentQueryService.list(severity, status, limit, offset);
    }

    @PostMapping("/incidents/analyze")
    public IncidentAnalysisResult analyze(@Valid @RequestBody IncidentAnalyzeRequest request) {
        IncidentCandidate candidate = incidentCorrelationService.correlate(request.anomalies());
        List<IncidentEvidence> evidence = candidate.evidence();
        List<Recommendation> recommendations = recommendationEngineService.generate(candidate, evidence);
        auditService.record("system", "INCIDENT_ANALYZED", "incident", candidate.incidentId(), candidate.summary());
        return new IncidentAnalysisResult(candidate, recommendations);
    }

    @GetMapping("/incidents/{incidentId}")
    public IncidentResponse get(@PathVariable UUID incidentId) {
        return incidentQueryService.get(incidentId);
    }

    @GetMapping("/incidents/{incidentId}/evidence")
    public List<IncidentEvidenceQueryResponse> evidence(@PathVariable UUID incidentId) {
        return incidentQueryService.evidence(incidentId);
    }

    @GetMapping("/incidents/{incidentId}/recommendations")
    public List<RecommendationSummaryResponse> recommendations(@PathVariable UUID incidentId) {
        return incidentQueryService.recommendations(incidentId);
    }

    @GetMapping("/reports/incidents/{incidentId}")
    public IncidentReportResponse report(@PathVariable UUID incidentId,
                                         @RequestParam(defaultValue = "markdown") String format) {
        if ("json".equalsIgnoreCase(format)) {
            return incidentQueryService.reportJson(incidentId);
        }
        return incidentQueryService.reportMarkdown(incidentId);
    }

    @GetMapping(value = "/reports/incidents/{incidentId}/markdown", produces = MediaType.TEXT_PLAIN_VALUE)
    public String reportMarkdown(@PathVariable UUID incidentId) {
        return incidentQueryService.reportMarkdown(incidentId).content();
    }

    @GetMapping(value = "/reports/incidents/{incidentId}/json", produces = MediaType.APPLICATION_JSON_VALUE)
    public String reportJson(@PathVariable UUID incidentId) {
        return incidentQueryService.reportJson(incidentId).content();
    }

    public record IncidentAnalysisResult(IncidentCandidate incident, List<Recommendation> recommendations) {
    }
}
