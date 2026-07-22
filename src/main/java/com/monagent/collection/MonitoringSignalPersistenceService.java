package com.monagent.collection;

import com.monagent.collection.model.NormalizedSignal;
import com.monagent.persistence.MonitoringSignalRepository;
import java.util.List;
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

    public List<NormalizedSignal> saveAll(List<NormalizedSignal> signals) {
        if (signals == null || signals.isEmpty()) {
            return List.of();
        }
        return repository.saveAllAndFlush(signals.stream().map(MonitoringSignalMapper::toEntity).toList())
                .stream()
                .map(MonitoringSignalMapper::toDomain)
                .toList();
    }
}
