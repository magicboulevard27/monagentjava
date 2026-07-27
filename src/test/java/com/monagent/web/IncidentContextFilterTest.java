package com.monagent.web;

import static org.assertj.core.api.Assertions.assertThat;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class IncidentContextFilterTest {

    @Test
    void addsIncidentIdToMdcForIncidentRoutes() throws ServletException, IOException {
        IncidentContextFilter filter = new IncidentContextFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/incidents/INC-123/evidence");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> seenIncidentId = new AtomicReference<>();
        FilterChain chain = (servletRequest, servletResponse) -> seenIncidentId.set(MDC.get("incidentId"));

        filter.doFilter(request, response, chain);

        assertThat(seenIncidentId.get()).isEqualTo("INC-123");
        assertThat(MDC.get("incidentId")).isNull();
    }

    @Test
    void leavesMdcEmptyForNonIncidentRoutes() throws ServletException, IOException {
        IncidentContextFilter filter = new IncidentContextFilter();
        MockHttpServletRequest request = new MockHttpServletRequest("GET", "/api/v1/services");
        MockHttpServletResponse response = new MockHttpServletResponse();
        AtomicReference<String> seenIncidentId = new AtomicReference<>();
        FilterChain chain = (servletRequest, servletResponse) -> seenIncidentId.set(MDC.get("incidentId"));

        filter.doFilter(request, response, chain);

        assertThat(seenIncidentId.get()).isNull();
        assertThat(MDC.get("incidentId")).isNull();
    }
}
