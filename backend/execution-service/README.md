# Execution Service

Executes workflows triggered by the Workflow Service. Consumes trigger events from Kafka, runs workflow steps, tracks execution state in PostgreSQL, uses Redis for caching, and publishes execution events back to Kafka.

## Tech Stack

- Java 21 / Spring Boot 3.5
- PostgreSQL (`flowforge_execution` database)
- Apache Kafka (consumer + producer)
- Redis (caching)
- JWT (token validation)

## Port

`8083`

## API Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/v1/executions/trigger/{workflowId}` | Bearer token | Manually trigger an execution |
| GET | `/api/v1/executions/{executionId}` | Bearer token | Get a single execution by ID |
| GET | `/api/v1/executions/workflow/{workflowId}` | Bearer token | Get all executions for a workflow |
| GET | `/api/v1/executions` | Bearer token | Get all executions for the current user |
| GET | `/api/v1/executions/filter?status=FAILED` | Bearer token | Filter executions by status |
| POST | `/api/v1/executions/{executionId}/retry` | Bearer token | Retry a failed execution from the failed step |
| GET | `/api/v1/executions/analytics/summary` | Bearer token | Dashboard analytics summary |
| GET | `/api/v1/executions/dlq` | Bearer token | View dead-letter queue entries |

## Kafka

- **Consumes:** `workflow.trigger` topic — receives trigger events from Workflow Service
- **Produces:** execution status events back to Kafka

## Local Setup

1. Create a PostgreSQL database named `flowforge_execution`.

2. Ensure Kafka is running on `localhost:9092` and Redis on `localhost:6379`.

3. Copy the example config and fill in your values:
   ```bash
   cp src/main/resources/application.properties.example src/main/resources/application.properties
   ```

4. Run the service:
   ```bash
   ./mvnw spring-boot:run
   ```

> **Note:** The JWT secret must be the same across all FlowForge services. The `workflow.service.url` must point to a running Workflow Service instance.

## Docker

Built and orchestrated via the root `docker-compose.yml`. The service depends on `postgres`, `kafka`, and `redis` being healthy before starting.
