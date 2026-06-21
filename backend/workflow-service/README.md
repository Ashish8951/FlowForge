# Workflow Service

Manages workflow definitions and triggers workflow executions for FlowForge. Publishes trigger events to Kafka which are consumed by the Execution Service.

## Tech Stack

- Java 21 / Spring Boot 3.5
- PostgreSQL (`flowforge_workflow` database)
- Apache Kafka (producer)
- JWT (token validation)

## Port

`8082`

## API Endpoints

### Workflows

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/v1/workflows` | Bearer token | Create a new workflow |
| GET | `/api/v1/workflows` | Bearer token | List all workflows |
| GET | `/api/v1/workflows/{id}` | Bearer token | Get a workflow by ID |
| PUT | `/api/v1/workflows/{id}` | Bearer token | Update a workflow |
| DELETE | `/api/v1/workflows/{id}` | Bearer token | Delete a workflow |
| POST | `/api/v1/workflows/{id}/activate` | Bearer token | Activate a workflow |
| POST | `/api/v1/workflows/{id}/trigger` | Bearer token | Manually trigger a workflow |

### Webhooks

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/v1/webhooks/{workflowId}` | Public | Receive external webhook to fire a workflow |

Webhook endpoint is public — intended for external systems (e.g. Stripe, GitHub). The workflow must be `ACTIVE` and have trigger type `WEBHOOK`.

## Local Setup

1. Create a PostgreSQL database named `flowforge_workflow`.

2. Ensure Kafka is running on `localhost:9092`.

3. Copy the example config and fill in your values:
   ```bash
   cp src/main/resources/application.properties.example src/main/resources/application.properties
   ```

4. Run the service:
   ```bash
   ./mvnw spring-boot:run
   ```

> **Note:** The JWT secret must be the same across all FlowForge services.

## Docker

Built and orchestrated via the root `docker-compose.yml`. The service depends on `postgres` and `kafka` being healthy before starting.
