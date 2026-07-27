package com.monagent.web;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 2)
public class IncidentContextFilter extends OncePerRequestFilter {

    private static final Pattern INCIDENT_PATH = Pattern.compile(".*/incidents/([A-Za-z0-9\\-]+)(?:/.*)?$");

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String incidentId = extractIncidentId(request.getRequestURI());
        if (incidentId != null) {
            MDC.put("incidentId", incidentId);
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            if (incidentId != null) {
                MDC.remove("incidentId");
            }
        }
    }

    private String extractIncidentId(String uri) {
        if (uri == null) {
            return null;
        }
        Matcher matcher = INCIDENT_PATH.matcher(uri);
        return matcher.matches() ? matcher.group(1) : null;
    }
}
