package com.monagent.collection.traces;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TraceRedactorTest {

    private final TraceRedactor redactor = new TraceRedactor();

    @Test
    void redactsSecretsAndConnections() {
        String input = "token=secret password=abc jdbc:postgresql://user:pass@host/db user@example.com";
        String output = redactor.redact(input);
        assertThat(output).doesNotContain("secret");
        assertThat(output).doesNotContain("user@example.com");
        assertThat(output).contains("[REDACTED_CONNECTION]");
    }
}
