package com.monagent.collection.health;

import com.monagent.collection.model.SignalSeverity;
import com.monagent.collection.model.SignalStatus;
import java.util.Locale;

final class HealthStatusMapper {

    private HealthStatusMapper() {
    }

    static SignalStatus mapStatus(String healthState, boolean dependencyIssue) {
        String normalized = healthState == null ? "" : healthState.trim().toLowerCase(Locale.ROOT);
        if ("down".equals(normalized)) {
            return SignalStatus.DOWN;
        }
        if ("degraded".equals(normalized) || dependencyIssue) {
            return SignalStatus.DEGRADED;
        }
        if ("up".equals(normalized)) {
            return SignalStatus.UP;
        }
        return SignalStatus.UNKNOWN;
    }

    static SignalSeverity mapSeverity(SignalStatus status) {
        return switch (status) {
            case DOWN -> SignalSeverity.CRITICAL;
            case DEGRADED -> SignalSeverity.HIGH;
            case UP, UNKNOWN, OK, WARN, ERROR -> SignalSeverity.NONE;
        };
    }
}
