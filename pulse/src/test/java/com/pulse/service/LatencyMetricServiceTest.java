package com.pulse.service;

import com.pulse.model.LogMetric;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LatencyMetricServiceTest {

    @Test
    void producesSnapshotEvery100Events() {

        LatencyMetricService service = new LatencyMetricService();

        List<LogMetric> snapshots = new ArrayList<>();
        for (int i = 1; i <= 200; i++) {
            service.record(i).ifPresent(snapshots::add);
        }

        assertEquals(2, snapshots.size());
        assertEquals(100, snapshots.get(0).getEventCount());
        assertEquals(200, snapshots.get(1).getEventCount());
    }

    @Test
    void noSnapshotUntilEvery100thEvent() {

        LatencyMetricService service = new LatencyMetricService();

        for (int i = 1; i <= 99; i++) {
            assertFalse(service.record(i).isPresent());
        }

        assertTrue(service.record(100).isPresent());

        for (int i = 101; i <= 199; i++) {
            assertFalse(service.record(i).isPresent());
        }

        assertTrue(service.record(200).isPresent());
    }

    @Test
    void snapshotComputesCorrectPercentiles() {

        LatencyMetricService service = new LatencyMetricService();

        Optional<LogMetric> snapshot = Optional.empty();
        for (int i = 1; i <= 100; i++) {
            snapshot = service.record(i);
        }

        assertTrue(snapshot.isPresent());

        LogMetric metric = snapshot.get();

        assertEquals(50.5, metric.getAverageLatency(), 0.001);
        assertEquals(1, metric.getMinLatency());
        assertEquals(100, metric.getMaxLatency());
        assertEquals(50, metric.getP50Latency());
        assertEquals(95, metric.getP95Latency());
        assertEquals(99, metric.getP99Latency());
    }
}