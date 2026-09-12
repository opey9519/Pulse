package com.pulse.consumer;

import com.pulse.model.LogEvent;
import com.pulse.repository.LogRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class ErrorConsumer {

    private static final Logger log = LoggerFactory.getLogger(ErrorConsumer.class);

    private final LogRepository logRepository;

    public ErrorConsumer(LogRepository logRepository) {
        this.logRepository = logRepository;
    }

    @KafkaListener(topics = "pulse-logs", groupId = "pulse-error-consumer")
    public void consume(LogEvent logEvent) {

        if ("ERROR".equalsIgnoreCase(logEvent.getLevel())) {

            log.info("Processing ERROR event: {}", logEvent.getMessage());

            logRepository.save(logEvent);

            log.info("ERROR event saved to PostgreSQL.");
        }
    }
}