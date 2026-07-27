package com.monagent.compose;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DockerComposeSpecificationTest {

    @Test
    void composeFileDefinesTheExpectedMvpStack() throws IOException {
        String compose = Files.readString(Path.of("docker-compose.yml"));

        assertThat(compose).contains("postgres:");
        assertThat(compose).contains("monagent:");
        assertThat(compose).contains("prometheus:");
        assertThat(compose).contains("opensearch:");
        assertThat(compose).contains("jaeger:");
        assertThat(compose).contains("ollama:");
        assertThat(compose).contains("profiles: [\"observability\"]");
        assertThat(compose).contains("profiles: [\"ai\"]");
        assertThat(compose).contains("healthcheck:");
        assertThat(compose).contains("monagent-net:");
        assertThat(compose).contains("postgres-data:");
        assertThat(compose).contains("monagent-data:");
        assertThat(compose).contains("monagent-logs:");
    }
}
