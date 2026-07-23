package com.monagent.api.service;

import com.monagent.domain.MonitoredService;
import jakarta.validation.Valid;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/services")
public class MonitoredServiceController {

    private static final Logger log = LoggerFactory.getLogger(MonitoredServiceController.class);

    private final MonitoredServiceService service;

    public MonitoredServiceController(MonitoredServiceService service) {
        this.service = service;
    }

    @GetMapping
    public List<MonitoredServiceResponse> list() {
        log.info("Listing monitored services");
        return service.list().stream().map(MonitoredServiceMapper::toResponse).toList();
    }

    @GetMapping("/{serviceId}")
    public MonitoredServiceResponse get(@PathVariable UUID serviceId) {
        log.info("Fetching monitored service serviceId={}", serviceId);
        return MonitoredServiceMapper.toResponse(service.get(serviceId));
    }

    @PostMapping
    public ResponseEntity<MonitoredServiceResponse> create(@Valid @RequestBody MonitoredServiceRequest request) {
        log.info("Creating monitored service serviceName={} environment={}", request.serviceName(), request.environment());
        MonitoredService created = service.create(request);
        return ResponseEntity.created(URI.create("/api/v1/services/" + created.serviceId()))
                .body(MonitoredServiceMapper.toResponse(created));
    }

    @PutMapping("/{serviceId}")
    public MonitoredServiceResponse update(@PathVariable UUID serviceId, @Valid @RequestBody MonitoredServiceRequest request) {
        log.info("Updating monitored service serviceId={} serviceName={} environment={}", serviceId, request.serviceName(), request.environment());
        return MonitoredServiceMapper.toResponse(service.update(serviceId, request));
    }

    @DeleteMapping("/{serviceId}")
    public ResponseEntity<Void> delete(@PathVariable UUID serviceId) {
        log.info("Deleting monitored service serviceId={}", serviceId);
        service.delete(serviceId);
        return ResponseEntity.noContent().build();
    }
}
