package com.pulse.controller;

import com.pulse.model.LogEvent;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;;

@RestController
@RequestMapping("/api/logs")
public class LogController {

    @PostMapping
    public ResponseEntity<LogEvent> createLog(@RequestBody LogEvent logEvent) {

        System.out.println("Received log:");
        System.out.println("Service: " + logEvent.getService());
        System.out.println("Level: " + logEvent.getLevel());
        System.out.println("Message: " + logEvent.getMessage());
        System.out.println("Latency: " + logEvent.getLatencyMs() + " ms");
        System.out.println("Timestamp: " + logEvent.getTimestamp());

        return ResponseEntity.ok(logEvent);
    }

}
