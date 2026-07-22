package com.monagent.collection.model;

import java.time.Instant;
import java.util.UUID;

public interface SourceSignal {

    UUID serviceId();

    String serviceName();

    String environment();

    Instant observedAt();
}
