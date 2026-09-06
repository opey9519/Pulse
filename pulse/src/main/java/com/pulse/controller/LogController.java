package com.pulse.controller;

import com.pulse.model.LogEvent;
import com.pulse.service.LogProducer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/logs")
public class LogController {

    private final LogProducer logProducer;

    public LogController(LogProducer logproducer) {
        this.logProducer = logproducer;
    }

    @PostMapping
    public ResponseEntity<LogEvent> createLog(@RequestBody LogEvent logEvent) {

        logProducer.send(logEvent);

        return ResponseEntity.ok(logEvent);
    }

}
