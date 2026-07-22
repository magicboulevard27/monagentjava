package com.monagent.collection;

import com.monagent.collection.model.NormalizedSignal;
import com.monagent.collection.model.SignalSeverity;
import com.monagent.collection.model.SignalStatus;
import com.monagent.collection.model.SourceType;
import com.monagent.persistence.MonitoringSignalEntity;
import java.util.UUID;

final class MonitoringSignalMapper {

    private MonitoringSignalMapper() {
    }

    static MonitoringSignalEntity toEntity(NormalizedSignal signal) {
        MonitoringSignalEntity entity = new MonitoringSignalEntity();
        entity.setSignalId(signal.signalId());
        entity.setServiceId(signal.serviceId());
        entity.setSourceType(signal.sourceType().name());
        entity.setSignalName(signal.signalName());
        entity.setSignalValue(signal.signalValue());
        entity.setUnit(signal.unit());
        entity.setStatus(signal.status().name());
        entity.setSeverity(signal.severity().name());
        entity.setCollectedAt(signal.collectedAt());
        entity.setRawReference(signal.rawReference());
        return entity;
    }

    static NormalizedSignal toDomain(MonitoringSignalEntity entity) {
        return new NormalizedSignal(
                entity.getSignalId(),
                entity.getServiceId(),
                SourceType.valueOf(entity.getSourceType()),
                entity.getSignalName(),
                entity.getSignalValue(),
                entity.getUnit(),
                SignalStatus.valueOf(entity.getStatus()),
                SignalSeverity.valueOf(entity.getSeverity()),
                entity.getCollectedAt(),
                entity.getRawReference());
    }
}
