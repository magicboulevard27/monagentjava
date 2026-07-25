package com.monagent.collection;

import com.monagent.collection.model.NormalizedSignal;
import com.monagent.persistence.MonitoringSignalRepository;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MonitoringSignalPersistenceService {

    private static final Logger log = LoggerFactory.getLogger(MonitoringSignalPersistenceService.class);

    private final MonitoringSignalRepository repository;

    public MonitoringSignalPersistenceService(MonitoringSignalRepository repository) {
        this.repository = repository;
    }

    public NormalizedSignal save(NormalizedSignal signal) {
        var entity = MonitoringSignalMapper.toEntity(signal);
        if (repository.existsById(entity.getSignalId())) {
            log.debug("Skipping duplicate monitoring signal {}", entity.getSignalId());
            return repository.findById(entity.getSignalId())
                    .map(MonitoringSignalMapper::toDomain)
                    .orElse(signal);
        }
        return MonitoringSignalMapper.toDomain(repository.saveAndFlush(entity));
    }

    public List<NormalizedSignal> saveAll(List<NormalizedSignal> signals) {
        if (signals == null || signals.isEmpty()) {
            return List.of();
        }
        Map<java.util.UUID, NormalizedSignal> uniqueSignals = new LinkedHashMap<>();
        for (NormalizedSignal signal : signals) {
            uniqueSignals.putIfAbsent(signal.signalId(), signal);
        }
        List<NormalizedSignal> savedSignals = new java.util.ArrayList<>(uniqueSignals.size());
        List<com.monagent.persistence.MonitoringSignalEntity> newEntities = new java.util.ArrayList<>();
        for (NormalizedSignal signal : uniqueSignals.values()) {
            var entity = MonitoringSignalMapper.toEntity(signal);
            if (repository.existsById(entity.getSignalId())) {
                log.debug("Skipping duplicate monitoring signal {}", entity.getSignalId());
                savedSignals.add(repository.findById(entity.getSignalId()).map(MonitoringSignalMapper::toDomain).orElse(signal));
            } else {
                newEntities.add(entity);
                savedSignals.add(signal);
            }
        }
        if (!newEntities.isEmpty()) {
            repository.saveAllAndFlush(newEntities);
        }
        return savedSignals;
    }
}
