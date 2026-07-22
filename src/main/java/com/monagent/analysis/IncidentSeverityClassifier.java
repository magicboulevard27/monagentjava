package com.monagent.analysis;

public final class IncidentSeverityClassifier {

    private IncidentSeverityClassifier() {
    }

    public static String classify(boolean serviceDown, boolean dataLoss, boolean customerImpact, int affectedServices) {
        if (serviceDown || dataLoss || affectedServices >= 4) {
            return "CRITICAL";
        }
        if (customerImpact || affectedServices == 3) {
            return "HIGH";
        }
        if (affectedServices == 2) {
            return "MEDIUM";
        }
        return "LOW";
    }
}
