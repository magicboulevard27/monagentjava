package com.monagent.collection.health;

import static org.assertj.core.api.Assertions.assertThat;

import com.monagent.collection.model.SignalSeverity;
import com.monagent.collection.model.SignalStatus;
import org.junit.jupiter.api.Test;

class HealthStatusMapperTest {

    @Test
    void mapsStatusAndSeverity() {
        assertThat(HealthStatusMapper.mapStatus("UP", false)).isEqualTo(SignalStatus.UP);
        assertThat(HealthStatusMapper.mapStatus("DOWN", false)).isEqualTo(SignalStatus.DOWN);
        assertThat(HealthStatusMapper.mapStatus("UP", true)).isEqualTo(SignalStatus.DEGRADED);
        assertThat(HealthStatusMapper.mapSeverity(SignalStatus.DOWN)).isEqualTo(SignalSeverity.CRITICAL);
    }
}
