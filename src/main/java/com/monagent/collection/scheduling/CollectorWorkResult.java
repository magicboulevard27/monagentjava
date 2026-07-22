package com.monagent.collection.scheduling;

public record CollectorWorkResult(
        CollectorJob job,
        boolean accepted,
        String status) {
}
