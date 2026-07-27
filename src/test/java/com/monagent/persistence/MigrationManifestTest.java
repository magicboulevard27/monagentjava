package com.monagent.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class MigrationManifestTest {

    @Test
    void migrationFilesArePresentAndOrdered() throws IOException {
        try (var paths = Files.list(Path.of("src/main/resources/db/migration"))) {
            List<String> migrationNames = paths
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .collect(Collectors.toList());

            assertThat(migrationNames).containsExactly(
                    "V1__create_monitoring_schema.sql",
                    "V2__add_event_hash_to_audit_logs.sql",
                    "V3__add_previous_hash_to_audit_logs.sql");
        }
    }
}
