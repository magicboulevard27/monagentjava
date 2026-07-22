package com.monagent.collection;

import com.monagent.collection.model.NormalizedSignal;
import com.monagent.persistence.MonitoringSignalRepository;
import org.springframework.stereotype.Service;

@Service
public class MonitoringSignalPersistenceService {

    private final MonitoringSignalRepository repository;

    public MonitoringSignalPersistenceService(MonitoringSignalRepository repository) {
        this.repository = repository;
    }

    public NormalizedSignal save(NormalizedSignal signal) {
        return MonitoringSignalMapper.toDomain(repository.saveAndFlush(MonitoringSignalMapper.toEntity(signal)));
    }
}
