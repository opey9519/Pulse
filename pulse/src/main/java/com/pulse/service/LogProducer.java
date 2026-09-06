package com.pulse.service;

import com.pulse.model.LogEvent;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class LogProducer {

    private static final String TOPIC = "pulse-logs";

    // Provided by Spring Kafka, send messages to Kafka
    // Key = String
    // Value = LogEvent
    private final KafkaTemplate<String, LogEvent> kafkaTemplate;

    public LogProducer(KafkaTemplate<String, LogEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(LogEvent logEvent) {
        kafkaTemplate.send(TOPIC, logEvent);
    }
}