package com.pulse.model;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "logs")
public class LogEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String service;

    @Column(nullable = false)
    private String level;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    private Long latencyMs;

    @Column(nullable = false)
    private Instant timestamp;

    public LogEvent() {
    }

    public LogEvent(
            Long id,
            String service,
            String level,
            String message,
            Long latencyMs,
            Instant timestamp) {
        this.id = id;
        this.service = service;
        this.level = level;
        this.message = message;
        this.latencyMs = latencyMs;
        this.timestamp = timestamp;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getService() {
        return service;
    }

    public void setService(String service) {
        this.service = service;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getLatencyMs() {
        return latencyMs;
    }

    public void setLatencyMs(Long latencyMs) {
        this.latencyMs = latencyMs;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }
}