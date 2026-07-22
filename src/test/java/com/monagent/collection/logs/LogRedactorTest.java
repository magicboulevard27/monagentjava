package com.monagent.collection.logs;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class LogRedactorTest {

    private final LogRedactor redactor = new LogRedactor();

    @Test
    void redactsSensitiveValues() {
        String input = "password=secret jdbc:postgresql://user:pass@host/db user@example.com Bearer abcdefghijklmnopqrstuvwxyz";
        String output = redactor.redact(input);
        assertThat(output).doesNotContain("secret");
        assertThat(output).contains("[REDACTED_PASSWORD]");
    }
}
