# Pulse

Pulse is a log-ingestion microservice. It accepts structured log events over
HTTP, streams them through Apache Kafka to real-time consumers, and persists
them to PostgreSQL.

```
HTTP POST /api/logs
        |
        v
  LogController  ->  LogProducer (KafkaTemplate)
        |                       |
        |                       v
        |        topic: pulse-logs  (3 partitions)
        |                       |
        +-----------+-----------+
                    |
        +-----------+-----------+
        |                       |
        v                       v
  ErrorConsumer          MetricsConsumer
  (ERROR events)         (latency percentiles)
        |                       |
        |                       v
        v               LogMetricRepository
  LogRepository         -> postgres: metrics
        |
        v
  postgres: logs
```

## Tech stack

- Java 21+ (built and run on OpenJDK 25)
- Spring Boot 4.1.1 (WebMVC, Actuator, Data JPA, Kafka)
- Spring Kafka 4.1.1
- Apache Kafka (KRaft, single-broker) via Docker Compose
- PostgreSQL 17 via Docker Compose
- Jackson 3 (`tools.jackson`) for JSON serialization

## Endpoints

| Method | Path          | Description                                        |
| ------ | ------------- | -------------------------------------------------- |
| GET    | `/api/health` | Returns `"Pulse is running"`                       |
| POST   | `/api/logs`   | Accepts a `LogEvent`, publishes it to `pulse-logs` |

### Example request

```bash
curl -X POST http://localhost:8080/api/logs \
  -H "Content-Type: application/json" \
  -d '{
    "service": "payment-api",
    "level": "ERROR",
    "message": "payment failed",
    "latencyMs": 250,
    "timestamp": "2026-09-07T12:00:00Z"
  }'
```

`LogEvent` fields: `id`, `service`, `level`, `message`, `latencyMs`, `timestamp`.

## What happens to a log

1. `LogController` passes the event to `LogProducer`, which publishes it to the
   `pulse-logs` topic.
2. **ErrorConsumer** keeps only `level=ERROR` events and saves them to the
   `logs` table.
3. **MetricsConsumer** accumulates `latencyMs` values in `LatencyMetricService`
   and, every 100 events, writes a latency snapshot to the `metrics` table
   (average, min, max, and P50 / P95 / P99) as well as printing it to the console.

## Getting started

Prerequisites: JDK 21+, Docker (for Kafka and PostgreSQL). The Maven wrapper
(`./mvnw`) is included, so no separate Maven install is required.

```bash
# 1. Start Kafka and PostgreSQL
docker compose up -d

# 2. Run the application (starts on http://localhost:8080)
./mvnw spring-boot:run

# 3. Verify
curl http://localhost:8080/api/health

# 4. Send a log
curl -X POST http://localhost:8080/api/logs \
  -H "Content-Type: application/json" \
  -d '{"service":"demo","level":"ERROR","message":"hello pulse","latencyMs":10,"timestamp":"2026-09-07T12:00:00Z"}'

# 5. Confirm it landed in PostgreSQL
docker exec pulse-postgres psql -U pulse -d pulse \
  -c "SELECT id, service, level, message, timestamp FROM logs;"
```

Both services must be up before the app starts: it connects to Kafka for the
consumers and to PostgreSQL at startup (`ddl-auto=update`).

## Configuration

Service connection settings live in
`src/main/resources/application.properties`:

```properties
# Kafka
spring.kafka.bootstrap-servers=localhost:9092
spring.kafka.producer.value-serializer=...JacksonJsonSerializer
spring.kafka.consumer.value-deserializer=...JacksonJsonDeserializer
spring.kafka.consumer.properties.spring.json.trusted.packages=com.pulse.model

# PostgreSQL (host port 5435, see docker-compose.yml)
spring.datasource.url=jdbc:postgresql://localhost:5435/pulse
spring.datasource.username=pulse
spring.datasource.password=pulse

# JPA
spring.jpa.hibernate.ddl-auto=update
```

Infrastructure is defined in `docker-compose.yml`:

- **Kafka** — single-node KRaft broker, port `9092`
- **PostgreSQL 17** — database `pulse`, user/password `pulse`/`pulse`,
  exposed on host port **5435**

## Data model

| Entity      | Table     | Notes                                                     |
| ----------- | --------- | --------------------------------------------------------- |
| `LogEvent`  | `logs`    | ERROR events saved by `ErrorConsumer` (`message` is TEXT) |
| `LogMetric` | `metrics` | Latency snapshots saved by `MetricsConsumer` every 100    |

## Project structure

```
pulse/
├── docker-compose.yml
├── pom.xml
└── src/
    ├── main/
    │   ├── java/com/pulse/
    │   │   ├── PulseApplication.java
    │   │   ├── config/KafkaConfig.java
    │   │   ├── consumer/{ErrorConsumer,MetricsConsumer}.java
    │   │   ├── controller/{LogController,HealthController}.java
    │   │   ├── model/{LogEvent,LogMetric}.java
    │   │   ├── repository/{LogRepository,LogMetricRepository}.java
    │   │   └── service/{LogProducer,LatencyMetricService}.java
    │   └── resources/application.properties
    └── test/java/com/pulse/PulseApplicationTests.java
```

## Testing

```bash
docker compose up -d        # Kafka + PostgreSQL must be running
./mvnw test
```

The Spring Boot context boots the consumers and JPA, so it connects to both
broker and database during tests.
