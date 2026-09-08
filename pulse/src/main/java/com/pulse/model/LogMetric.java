package com.pulse.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "metrics")
public class LogMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private long eventCount;

    private double averageLatency;

    private long p50Latency;

    private long p95Latency;

    private long p99Latency;

    private long minLatency;

    private long maxLatency;

    @Column(nullable = false)
    private Instant timestamp;

    public LogMetric() {
    }

    public Long getId() {
        return id;
    }

    public long getEventCount() {
        return eventCount;
    }

    public void setEventCount(long eventCount) {
        this.eventCount = eventCount;
    }

    public double getAverageLatency() {
        return averageLatency;
    }

    public void setAverageLatency(double averageLatency) {
        this.averageLatency = averageLatency;
    }

    public long getP50Latency() {
        return p50Latency;
    }

    public void setP50Latency(long p50Latency) {
        this.p50Latency = p50Latency;
    }

    public long getP95Latency() {
        return p95Latency;
    }

    public void setP95Latency(long p95Latency) {
        this.p95Latency = p95Latency;
    }

    public long getP99Latency() {
        return p99Latency;
    }

    public void setP99Latency(long p99Latency) {
        this.p99Latency = p99Latency;
    }

    public long getMinLatency() {
        return minLatency;
    }

    public void setMinLatency(long minLatency) {
        this.minLatency = minLatency;
    }

    public long getMaxLatency() {
        return maxLatency;
    }

    public void setMaxLatency(long maxLatency) {
        this.maxLatency = maxLatency;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}