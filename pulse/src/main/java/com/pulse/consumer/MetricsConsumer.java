package com.pulse.consumer;

import com.pulse.model.LogEvent;
import com.pulse.model.LogMetric;
import com.pulse.repository.LogMetricRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class MetricsConsumer {

    private final List<Long> latencies = new ArrayList<>();

    private final LogMetricRepository logMetricRepository;

    public MetricsConsumer(LogMetricRepository logMetricRepository) {
        this.logMetricRepository = logMetricRepository;
    }

    @KafkaListener(topics = "pulse-logs", groupId = "pulse-metrics-consumer")
    public synchronized void consume(LogEvent logEvent) {

        if (logEvent.getLatencyMs() == null) {
            return;
        }

        latencies.add(logEvent.getLatencyMs());

        if (latencies.size() % 100 == 0) {
            printMetrics();
            saveMetrics();
        }
    }

    private void saveMetrics() {

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

        logMetricRepository.save(metric);

        System.out.println(
                "Latency metrics snapshot saved to PostgreSQL.");
    }

    private void printMetrics() {

        List<Long> sorted = new ArrayList<>(latencies);
        Collections.sort(sorted);

        double average = sorted.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0);

        long min = sorted.get(0);
        long max = sorted.get(sorted.size() - 1);

        long p50 = percentile(sorted, 50);
        long p95 = percentile(sorted, 95);
        long p99 = percentile(sorted, 99);

        System.out.println();
        System.out.println("========== LATENCY METRICS ==========");
        System.out.println("Samples: " + sorted.size());
        System.out.println("Average: " + average + " ms");
        System.out.println("P50:     " + p50 + " ms");
        System.out.println("P95:     " + p95 + " ms");
        System.out.println("P99:     " + p99 + " ms");
        System.out.println("Min:     " + min + " ms");
        System.out.println("Max:     " + max + " ms");
        System.out.println("=====================================");
        System.out.println();
    }

    private long percentile(List<Long> sorted, double percentile) {

        int index = (int) Math.ceil(
                percentile / 100.0 * sorted.size()) - 1;

        index = Math.max(0, Math.min(index, sorted.size() - 1));

        return sorted.get(index);
    }
}