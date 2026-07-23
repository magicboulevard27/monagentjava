package com.monagent.api.service;

import com.monagent.domain.MonitoredService;
import com.monagent.persistence.MonitoredServiceEntity;
import com.monagent.persistence.MonitoredServiceRepository;
import jakarta.persistence.EntityNotFoundException;
import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class MonitoredServiceService {

    private static final Logger log = LoggerFactory.getLogger(MonitoredServiceService.class);

    private static final List<String> ALLOWED_ENVIRONMENTS = List.of("development", "testing", "staging", "production");

    private final MonitoredServiceRepository repository;

    public MonitoredServiceService(MonitoredServiceRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<MonitoredService> list() {
        return repository.findAll().stream().map(MonitoredServiceMapper::toDomain).toList();
    }

    @Transactional(readOnly = true)
    public MonitoredService get(UUID serviceId) {
        return repository.findById(serviceId)
                .map(MonitoredServiceMapper::toDomain)
                .orElseThrow(() -> new EntityNotFoundException("Service not found: " + serviceId));
    }

    public MonitoredService create(MonitoredServiceRequest request) {
        validate(request);
        MonitoredServiceEntity entity = new MonitoredServiceEntity(UUID.randomUUID());
        MonitoredServiceMapper.applyRequest(entity, request);
        entity.setCreatedAt(Instant.now());
        entity.setUpdatedAt(Instant.now());
        return MonitoredServiceMapper.toDomain(repository.saveAndFlush(entity));
    }

    public MonitoredService update(UUID serviceId, MonitoredServiceRequest request) {
        validate(request);
        MonitoredServiceEntity entity = repository.findById(serviceId)
                .orElseThrow(() -> new EntityNotFoundException("Service not found: " + serviceId));
        MonitoredServiceMapper.applyRequest(entity, request);
        entity.setUpdatedAt(Instant.now());
        return MonitoredServiceMapper.toDomain(repository.saveAndFlush(entity));
    }

    public void delete(UUID serviceId) {
        if (!repository.existsById(serviceId)) {
            throw new EntityNotFoundException("Service not found: " + serviceId);
        }
        repository.deleteById(serviceId);
    }

    private void validate(MonitoredServiceRequest request) {
        if (!ALLOWED_ENVIRONMENTS.contains(request.environment())) {
            throw new IllegalArgumentException("Unsupported environment: " + request.environment());
        }
        URI.create(request.healthUrl());
    }
}
