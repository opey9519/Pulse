package com.loggenerator.generator;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Random;

public class LogGenerator {

    // Local backend API endpoint (PULSE_URL can override, e.g. http://api:8080 in compose)
    private static final String API_URL =
            System.getenv().getOrDefault("PULSE_URL", "http://localhost:8080") + "/api/logs";

    // Available services expected
    private static final String[] SERVICES = {
            "payment-service",
            "user-service",
            "inventory-service",
            "authentication-service"
    };

    // Available levels expected (INFO occurence = 3 on purpose)
    private static final String[] LEVELS = {
            "INFO",
            "INFO",
            "INFO",
            "WARN",
            "ERROR"
    };

    private static final Random RANDOM = new Random();

    private static final ObjectMapper OBJECT_MAPPER =
            new ObjectMapper().findAndRegisterModules();

    private static final HttpClient HTTP_CLIENT = HttpClient.newHttpClient();

    public static void main(String[] args) throws Exception {

        int events = 100; // # of events
        int rate = 10; // rate of events/second

        for (int i = 0; i < args.length; i++) {

            if ("--events".equals(args[i]) && i + 1 < args.length) {
                events = Integer.parseInt(args[++i]);
            }

            else if ("--rate".equals(args[i]) && i + 1 < args.length) {
                rate = Integer.parseInt(args[++i]);
            }
        }

        System.out.println("Starting Pulse Log Generator");
        System.out.println("Events: " + events);
        System.out.println("Rate: " + rate + " events/sec");
        System.out.println();

        long delayMs = rate > 0
                ? 1000L / rate
                : 0;

        for (int i = 0; i < events; i++) {

            sendLog(i);

            if (delayMs > 0) {
                Thread.sleep(delayMs);
            }
        }

        System.out.println();
        System.out.println("Finished generating logs.");
    }

    private static void sendLog(int eventNumber)
            throws Exception {

        // Generate service
        String service = SERVICES[RANDOM.nextInt(SERVICES.length)];
        // Generate level
        String level = LEVELS[RANDOM.nextInt(LEVELS.length)];
        // generate message
        String message = generateMessage(level, service);
        // generate latency in ms
        long latencyMs = RANDOM.nextInt(1000);

        LogEvent logEvent = new LogEvent(
                service,
                level,
                message,
                latencyMs,
                Instant.now());

        String json = OBJECT_MAPPER.writeValueAsString(logEvent);

        HttpRequest request = HttpRequest.newBuilder().uri(URI.create(API_URL))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json))
                .build();

        HttpResponse<String> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());

        if (eventNumber % 100 == 0) {

            System.out.println(
                    "Generated event "
                            + eventNumber
                            + " | "
                            + level
                            + " | "
                            + service
                            + " | HTTP "
                            + response.statusCode());
        }
    }

    // Generates message based on level and service
    private static String generateMessage(
            String level,
            String service) {

        if ("ERROR".equals(level)) {

            return switch (service) {

                case "payment-service" ->
                    "Payment processing failed";

                case "user-service" ->
                    "User lookup failed";

                case "inventory-service" ->
                    "Inventory service unavailable";

                case "authentication-service" ->
                    "Authentication failed";

                default ->
                    "Application error";
            };
        }

        if ("WARN".equals(level)) {

            return switch (service) {

                case "payment-service" ->
                    "Payment processing is slow";

                case "user-service" ->
                    "User lookup is slow";

                case "inventory-service" ->
                    "Inventory lookup is slow";

                case "authentication-service" ->
                    "Authentication request is slow";

                default ->
                    "Application warning";
            };
        }

        return switch (service) {

            case "payment-service" ->
                "Payment completed successfully";

            case "user-service" ->
                "User request completed";

            case "inventory-service" ->
                "Inventory lookup completed";

            case "authentication-service" ->
                "User authenticated successfully";

            default ->
                "Request completed";
        };
    }

    // Model
    static class LogEvent {

        private String service;
        private String level;
        private String message;
        private long latencyMs;
        private Instant timestamp;

        public LogEvent(
                String service,
                String level,
                String message,
                long latencyMs,
                Instant timestamp) {
            this.service = service;
            this.level = level;
            this.message = message;
            this.latencyMs = latencyMs;
            this.timestamp = timestamp;
        }

        public String getService() {
            return service;
        }

        public String getLevel() {
            return level;
        }

        public String getMessage() {
            return message;
        }

        public long getLatencyMs() {
            return latencyMs;
        }

        public Instant getTimestamp() {
            return timestamp;
        }
    }
}