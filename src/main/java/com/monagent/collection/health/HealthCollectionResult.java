package com.monagent.collection.health;

import com.monagent.collection.model.NormalizedSignal;

public record HealthCollectionResult(
        NormalizedSignal signal,
        boolean persisted) {
}
