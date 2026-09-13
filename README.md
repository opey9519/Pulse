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

Prerequisites: Docker. Kafka, PostgreSQL, the API, and the log generator all run
through the root-level `docker-compose.yml`. For local development outside
Docker, a JDK 21+ and the included Maven wrapper (`./mvnw`) are used instead.

### Run everything

```bash
# 1. Build and start all services (kafka, postgres, api, loggenerator)
docker compose up --build

# 2. Verify the API
curl http://localhost:8080/api/health

# 3. Confirm logs landed in PostgreSQL
docker exec pulse-postgres psql -U pulse -d pulse \
  -c "SELECT id, service, level, message, timestamp FROM logs;"
```

`docker compose up` also starts `loggenerator`, which POSTs a batch of synthetic
logs to the API and then exits. Watch its progress with:

```bash
docker logs -f pulse-loggenerator
```

To run the generator manually, e.g. with 500 events at 50/sec:

```bash
docker compose run --rm --replace loggenerator \
  -e EVENTS=500 -e RATE=50
```

### Run the API locally (development)

```bash
# 1. Start only Kafka and PostgreSQL
docker compose up -d kafka postgres

# 2. Add a hosts alias so the local app can resolve the broker hostname
#    (Kafka advertises itself as kafka:9092 on the compose network):
#    /etc/hosts:  127.0.0.1  kafka

# 3. Run the application (starts on http://localhost:8080)
cd pulse
./mvnw spring-boot:run
```

The infra services must be healthy before the app starts: it connects to Kafka
for the consumers and to PostgreSQL at startup (`ddl-auto=update`).

### Send a log by hand

```bash
curl -X POST http://localhost:8080/api/logs \
  -H "Content-Type: application/json" \
  -d '{"service":"demo","level":"ERROR","message":"hello pulse","latencyMs":10,"timestamp":"2026-09-07T12:00:00Z"}'
```

## Configuration

Service connection settings live in
`src/main/resources/application.properties` (used for local runs):

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

Infrastructure and services are defined in the root `docker-compose.yml`:

- **Kafka** — single-node KRaft broker, port `9092`. Advertises
  `PLAINTEXT://kafka:9092` so containers reach it over the compose network;
  host access to the broker is via `docker exec` commands.
- **PostgreSQL 17** — database `pulse`, user/password `pulse`/`pulse`,
  exposed on host port **5435**
- **api** — the Spring Boot app, host port `8080`. Connections are injected via
  `SPRING_KAFKA_BOOTSTRAP_SERVERS` and `SPRING_DATASOURCE_*` environment
  variables (overriding the localhost values above).
- **loggenerator** — runs a batch of synthetic logs against the API, then exits.
  Uses `PULSE_URL` (default `http://localhost:8080`), `EVENTS`, and `RATE`.

## Data model

| Entity      | Table     | Notes                                                     |
| ----------- | --------- | --------------------------------------------------------- |
| `LogEvent`  | `logs`    | ERROR events saved by `ErrorConsumer` (`message` is TEXT) |
| `LogMetric` | `metrics` | Latency snapshots saved by `MetricsConsumer` every 100    |

## Project structure

```
pulse/
├── Dockerfile
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

loggenerator/
├── Dockerfile
├── pom.xml
└── src/main/java/com/loggenerator/
    ├── Main.java
    └── generator/LogGenerator.java
```

The root `docker-compose.yml` orchestrates `kafka`, `postgres`, `api` (built
from `pulse/`), and `loggenerator`.

## Testing

```bash
docker compose up -d kafka postgres   # Kafka + PostgreSQL must be running
./mvnw test                           # localhost runs also need the /etc/hosts
                                      # alias above (127.0.0.1 kafka)
```

The Spring Boot context boots the consumers and JPA, so it connects to both
broker and database during tests.
