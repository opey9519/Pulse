package com.pulse.service;

import com.pulse.model.LogMetric;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Service
public class LatencyMetricService {

    private static final int SNAPSHOT_EVERY = 100;

    private final List<Long> latencies = new ArrayList<>();

    public synchronized Optional<LogMetric> record(long latencyMs) {

        latencies.add(latencyMs);

        if (latencies.size() % SNAPSHOT_EVERY != 0) {
            return Optional.empty();
        }

        return Optional.of(buildSnapshot());
    }

    public synchronized LogMetric buildSnapshot() {

        List<Long> sorted = new ArrayList<>(latencies);
        Collections.sort(sorted);

        double average = sorted.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0);

        LogMetric metric = new LogMetric();
        metric.setEventCount(sorted.size());
        metric.setAverageLatency(average);
        metric.setP50Latency(percentile(sorted, 50));
        metric.setP95Latency(percentile(sorted, 95));
        metric.setP99Latency(percentile(sorted, 99));
        metric.setMinLatency(sorted.get(0));
        metric.setMaxLatency(sorted.get(sorted.size() - 1));
        metric.setTimestamp(Instant.now());

        return metric;
    }

    private long percentile(List<Long> sorted, double percentile) {

        int index = (int) Math.ceil(
                percentile / 100.0 * sorted.size()) - 1;

        index = Math.max(0, Math.min(index, sorted.size() - 1));

        return sorted.get(index);
    }
}