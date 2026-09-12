package com.pulse.consumer;

import com.pulse.model.LogEvent;
import com.pulse.model.LogMetric;
import com.pulse.repository.LogMetricRepository;
import com.pulse.service.LatencyMetricService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class MetricsConsumer {

    private static final Logger log = LoggerFactory.getLogger(MetricsConsumer.class);

    private final LatencyMetricService latencyMetricService;
    private final LogMetricRepository logMetricRepository;

    public MetricsConsumer(LatencyMetricService latencyMetricService, LogMetricRepository logMetricRepository) {
        this.latencyMetricService = latencyMetricService;
        this.logMetricRepository = logMetricRepository;
    }

    @KafkaListener(topics = "pulse-logs", groupId = "pulse-metrics-consumer")
    public void consume(LogEvent logEvent) {

        if (logEvent.getLatencyMs() == null) {
            return;
        }

        latencyMetricService.record(logEvent.getLatencyMs())
                .ifPresent(metric -> {
                    printMetrics(metric);
                    logMetricRepository.save(metric);
                    log.info("Latency metrics snapshot saved to PostgreSQL.");
                });
    }

    private void printMetrics(LogMetric metric) {

        System.out.println();
        System.out.println("========== LATENCY METRICS ==========");
        System.out.println("Samples: " + metric.getEventCount());
        System.out.println("Average: " + metric.getAverageLatency() + " ms");
        System.out.println("P50:     " + metric.getP50Latency() + " ms");
        System.out.println("P95:     " + metric.getP95Latency() + " ms");
        System.out.println("P99:     " + metric.getP99Latency() + " ms");
        System.out.println("Min:     " + metric.getMinLatency() + " ms");
        System.out.println("Max:     " + metric.getMaxLatency() + " ms");
        System.out.println("=====================================");
        System.out.println();
    }
}