package com.monagent.api.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.monagent.persistence.IncidentEntity;
import com.monagent.persistence.IncidentEvidenceRepository;
import com.monagent.persistence.IncidentRepository;
import com.monagent.persistence.RecommendationRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class IncidentQueryServiceTest {

    @Test
    void listsIncidentsAndBuildsReports() {
        IncidentRepository incidentRepository = mock(IncidentRepository.class);
        IncidentEvidenceRepository evidenceRepository = mock(IncidentEvidenceRepository.class);
        RecommendationRepository recommendationRepository = mock(RecommendationRepository.class);
        IncidentReportRenderer renderer = new IncidentReportRenderer(new com.fasterxml.jackson.databind.ObjectMapper());
        IncidentQueryService service = new IncidentQueryService(incidentRepository, evidenceRepository, recommendationRepository, renderer);

        UUID incidentId = UUID.randomUUID();
        IncidentEntity incident = new IncidentEntity();
        incident.setIncidentId(incidentId);
        incident.setTitle("test incident");
        incident.setSeverity("HIGH");
        incident.setStatus("ACTIVE");
        incident.setAffectedServices("[\"service-a\"]");
        incident.setStartTime(Instant.parse("2026-07-22T10:00:00Z"));
        incident.setDetectedAt(Instant.parse("2026-07-22T10:05:00Z"));
        incident.setSummary("cpu saturation");
        when(incidentRepository.findAll()).thenReturn(List.of(incident));
        when(incidentRepository.findById(incidentId)).thenReturn(java.util.Optional.of(incident));
        when(evidenceRepository.findAll()).thenReturn(List.of());
        when(recommendationRepository.findAll()).thenReturn(List.of());

        assertThat(service.list(null, null, 10, 0)).hasSize(1);
        assertThat(service.reportMarkdown(incidentId).content()).contains("Incident");
        assertThat(service.reportJson(incidentId).content()).contains("incident");
    }
}
