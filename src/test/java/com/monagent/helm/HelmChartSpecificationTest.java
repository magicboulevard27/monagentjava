package com.monagent.helm;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class HelmChartSpecificationTest {

    @Test
    void chartIncludesTheExpectedWorkloadsAndRecoveryResources() throws IOException {
        try (var paths = Files.list(Path.of("helm/monagentjava/templates"))) {
            var templateNames = paths
                    .map(path -> path.getFileName().toString())
                    .sorted()
                    .collect(Collectors.toList());

            assertThat(templateNames).contains(
                    "deployment-api.yaml",
                    "deployment-collector.yaml",
                    "deployment-analysis.yaml",
                    "deployment-notification.yaml",
                    "role-collector.yaml",
                    "rolebinding-collector.yaml",
                    "hpa.yaml",
                    "pdb.yaml",
                    "networkpolicy.yaml",
                    "service.yaml",
                    "serviceaccount.yaml");
        }
    }

    @Test
    void collectorRoleIsNamespaceScopedAndLeastPrivilege() throws IOException {
        String role = Files.readString(Path.of("helm/monagentjava/templates/role-collector.yaml"));
        String roleBinding = Files.readString(Path.of("helm/monagentjava/templates/rolebinding-collector.yaml"));

        assertThat(role).contains("kind: Role");
        assertThat(role).contains("resources: [\"pods\", \"pods/log\", \"events\", \"namespaces\"]");
        assertThat(roleBinding).contains("kind: RoleBinding");
        assertThat(roleBinding).contains("kind: Role");
        assertThat(roleBinding).contains("-collector");
    }
}
