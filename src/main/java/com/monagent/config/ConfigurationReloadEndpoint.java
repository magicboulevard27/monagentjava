package com.monagent.config;

import java.io.IOException;
import java.util.Map;
import org.springframework.boot.actuate.endpoint.annotation.Endpoint;
import org.springframework.boot.actuate.endpoint.annotation.ReadOperation;
import org.springframework.boot.actuate.endpoint.annotation.WriteOperation;
import org.springframework.stereotype.Component;

@Component
@Endpoint(id = "configreload")
public class ConfigurationReloadEndpoint {

    private final ConfigurationReloadService reloadService;

    public ConfigurationReloadEndpoint(ConfigurationReloadService reloadService) {
        this.reloadService = reloadService;
    }

    @ReadOperation
    public Map<String, Object> current() {
        ConfigurationSnapshot snapshot = reloadService.current();
        return Map.of(
                "timezone", snapshot.runtime().timezone(),
                "logDirectory", snapshot.runtime().logDirectory(),
                "dataDirectory", snapshot.runtime().dataDirectory(),
                "ollamaModel", snapshot.integrations().ollama().model(),
                "enabledChannels", snapshot.integrations().notifications().enabledChannels());
    }

    @WriteOperation
    public Map<String, Object> reload() throws IOException {
        ConfigurationSnapshot snapshot = reloadService.reload();
        return Map.of(
                "reloaded", true,
                "timezone", snapshot.runtime().timezone(),
                "logDirectory", snapshot.runtime().logDirectory(),
                "dataDirectory", snapshot.runtime().dataDirectory());
    }
}
