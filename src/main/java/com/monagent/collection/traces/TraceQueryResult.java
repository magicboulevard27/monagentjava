package com.monagent.collection.traces;

import java.util.Map;

public record TraceQueryResult(
        Map<String, Object> payload) {
}
