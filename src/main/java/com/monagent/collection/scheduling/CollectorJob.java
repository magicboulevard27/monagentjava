package com.monagent.collection.scheduling;

public record CollectorJob(
        CollectorJobType type,
        String serviceId) {
}
