package com.pulse.consumer;

import com.pulse.model.LogEvent;
import com.pulse.repository.LogRepository;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Service;

@Service
public class ErrorConsumer {

    private final LogRepository logRepository;

    public ErrorConsumer(LogRepository logRepository) {
        this.logRepository = logRepository;
    }

    @KafkaListener(topics = "pulse-logs", groupId = "pulse-error-consumer")
    public void consume(LogEvent logEvent) {

        if ("ERROR".equalsIgnoreCase(logEvent.getLevel())) {

            System.out.println("Processing ERROR event:");
            System.out.println(logEvent.getMessage());

            logRepository.save(logEvent);

            System.out.println(
                    "ERROR event saved to PostgreSQL.");
        }
    }
}