package com.monagent.api.service;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class MonitoredServiceControllerIT {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser
    void createListUpdateAndDeleteService() throws Exception {
        String body = """
                {
                  "serviceName": "order-service",
                  "environment": "staging",
                  "ownerTeam": "payments",
                  "healthUrl": "https://order-service.internal/actuator/health",
                  "prometheusJobName": "order-service",
                  "logIndexPattern": "logs-order-*",
                  "traceServiceName": "order-service",
                  "kubernetesNamespace": "prod",
                  "kubernetesWorkloadName": "order-service",
                  "alertChannels": ["slack", "email"],
                  "enabled": true
                }
                """;

        String location = mockMvc.perform(post("/api/v1/services")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.serviceName").value("order-service"))
                .andReturn()
                .getResponse()
                .getHeader("Location");

        mockMvc.perform(get("/api/v1/services"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].serviceName").value("order-service"));

        String updateBody = """
                {
                  "serviceName": "order-service",
                  "environment": "production",
                  "ownerTeam": "payments",
                  "healthUrl": "https://order-service.internal/actuator/health",
                  "prometheusJobName": "order-service",
                  "logIndexPattern": "logs-order-*",
                  "traceServiceName": "order-service",
                  "kubernetesNamespace": "prod",
                  "kubernetesWorkloadName": "order-service",
                  "alertChannels": ["slack"],
                  "enabled": false
                }
                """;

        mockMvc.perform(put(location)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(updateBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.environment").value("production"))
                .andExpect(jsonPath("$.enabled").value(false));

        mockMvc.perform(delete(location))
                .andExpect(status().isNoContent());

        mockMvc.perform(get(location))
                .andExpect(status().isNotFound());
    }
}
