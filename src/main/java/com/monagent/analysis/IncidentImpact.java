package com.monagent.analysis;

public record IncidentImpact(
        int affectedServiceCount,
        boolean customerImpact,
        boolean blastRadiusElevated) {
}
