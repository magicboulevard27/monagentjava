package com.monagent.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;
import org.springframework.stereotype.Service;

@Service
public class ConfigurationReloadService {

    private final ReloadableConfigurationProperties reloadableConfigurationProperties;
    private final AtomicReference<ConfigurationSnapshot> snapshot;

    public ConfigurationReloadService(
            RuntimeProperties runtimeProperties,
            IntegrationProperties integrationProperties,
            ReloadableConfigurationProperties reloadableConfigurationProperties) {
        this.reloadableConfigurationProperties = reloadableConfigurationProperties;
        this.snapshot = new AtomicReference<>(new ConfigurationSnapshot(runtimeProperties, integrationProperties));
    }

    public ConfigurationSnapshot current() {
        return snapshot.get();
    }

    public ConfigurationSnapshot reload() throws IOException {
        Path overrideFile = Path.of(reloadableConfigurationProperties.file());
        if (!Files.exists(overrideFile)) {
            return snapshot.get();
        }

        Properties overrides = new Properties();
        try (InputStream inputStream = Files.newInputStream(overrideFile)) {
            overrides.load(inputStream);
        }

        ConfigurationSnapshot current = snapshot.get();
        RuntimeProperties runtime = new RuntimeProperties(
                overrides.getProperty("monagent.runtime.timezone", current.runtime().timezone()),
                overrides.getProperty("monagent.runtime.log-directory", current.runtime().logDirectory()),
                overrides.getProperty("monagent.runtime.data-directory", current.runtime().dataDirectory()));

        IntegrationProperties integrations = new IntegrationProperties(
                new IntegrationProperties.Database(
                        overrides.getProperty("monagent.integrations.database.url", current.integrations().database().url()),
                        overrides.getProperty("monagent.integrations.database.username", current.integrations().database().username()),
                        overrides.getProperty("monagent.integrations.database.password", current.integrations().database().password()),
                        overrides.getProperty("monagent.integrations.database.schema", current.integrations().database().schema())),
                new IntegrationProperties.Ollama(
                        java.net.URI.create(overrides.getProperty("monagent.integrations.ollama.base-url", current.integrations().ollama().baseUrl().toString())),
                        overrides.getProperty("monagent.integrations.ollama.model", current.integrations().ollama().model()),
                        Integer.valueOf(overrides.getProperty("monagent.integrations.ollama.request-timeout-seconds", current.integrations().ollama().requestTimeoutSeconds().toString()))),
                new IntegrationProperties.Notifications(
                        java.util.Arrays.asList(
                                overrides.getProperty("monagent.integrations.notifications.enabled-channels.0", current.integrations().notifications().enabledChannels().get(0)),
                                overrides.getProperty("monagent.integrations.notifications.enabled-channels.1", current.integrations().notifications().enabledChannels().size() > 1 ? current.integrations().notifications().enabledChannels().get(1) : current.integrations().notifications().enabledChannels().get(0))),
                        overrides.getProperty("monagent.integrations.notifications.from-address", current.integrations().notifications().fromAddress())),
                new IntegrationProperties.Auth(
                        overrides.getProperty("monagent.integrations.auth.issuer-uri", current.integrations().auth().issuerUri()),
                        overrides.getProperty("monagent.integrations.auth.audience", current.integrations().auth().audience()),
                        overrides.getProperty("monagent.integrations.auth.default-role-claim", current.integrations().auth().defaultRoleClaim())),
                new IntegrationProperties.Observability(
                        overrides.getProperty("monagent.integrations.observability.metrics-export-endpoint", current.integrations().observability().metricsExportEndpoint()),
                        overrides.getProperty("monagent.integrations.observability.tracing-export-endpoint", current.integrations().observability().tracingExportEndpoint())));

        ConfigurationSnapshot updated = new ConfigurationSnapshot(runtime, integrations);
        snapshot.set(updated);
        return updated;
    }
}
