package com.monagent.api.service;

import com.monagent.analysis.AnomalyOutcome;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record IncidentAnalyzeRequest(
        @NotEmpty @Valid List<AnomalyOutcome> anomalies) {
}
