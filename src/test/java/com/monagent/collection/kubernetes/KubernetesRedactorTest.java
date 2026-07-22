package com.monagent.collection.kubernetes;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class KubernetesRedactorTest {

    private final KubernetesRedactor redactor = new KubernetesRedactor();

    @Test
    void redactsSecretsAndEmails() {
        String input = "token=secret clientSecret=abc bearer abcdefghijklmnopqrstuvwxyz user@example.com";
        String output = redactor.redact(input);
        assertThat(output).doesNotContain("user@example.com");
        assertThat(output).contains("[REDACTED]");
    }
}
