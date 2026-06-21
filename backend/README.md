# FlowForge — Backend

FlowForge is a workflow automation platform. The backend is built as a set of Spring Boot microservices that communicate over Kafka and share a PostgreSQL instance.

## Architecture

```
┌─────────────────────────────────────────────────────────┐
│                    Client / Frontend                     │
└────────────────────────┬────────────────────────────────┘
                         │ REST
          ┌──────────────┼──────────────┐
          ▼              ▼              ▼
   ┌─────────────┐ ┌─────────────┐ ┌─────────────────┐
   │ auth-service│ │workflow-svc │ │ execution-svc   │
   │  :8081      │ │  :8082      │ │  :8083          │
   └──────┬──────┘ └──────┬──────┘ └────────┬────────┘
          │               │   Kafka trigger  │
          │               └─────────────────►│
          │                                  │
          ▼               ▼                  ▼
   ┌──────────────────────────────────────────────────┐
   │              PostgreSQL  |  Kafka  |  Redis       │
   └──────────────────────────────────────────────────┘
```

## Services

| Service | Port | Description | Readme |
|---------|------|-------------|--------|
| [auth-service](./auth-service) | 8081 | User registration, login, JWT issuance | [README](./auth-service/README.md) |
| [workflow-service](./workflow-service) | 8082 | Workflow CRUD, activation, webhook triggers | [README](./workflow-service/README.md) |
| [execution-service](./execution-service) | 8083 | Workflow execution engine, retry, analytics | [README](./execution-service/README.md) |

## Infrastructure

| Component | Port | Purpose |
|-----------|------|---------|
| PostgreSQL 16 | 5432 | Persistent storage for all services (separate databases per service) |
| Apache Kafka | 9092 | Async event bus between workflow-service and execution-service |
| Zookeeper | 2181 | Kafka coordination |
| Redis 7 | 6379 | Caching layer for execution-service |

## Running Locally

### Option 1 — Docker Compose (recommended)

Starts all infrastructure and services together:

```bash
cd backend
docker-compose up --build
```

### Option 2 — Services individually

1. Start infrastructure only:
   ```bash
   cd backend
   docker-compose up postgres kafka zookeeper redis
   ```

2. For each service, copy its example config and set your values:
   ```bash
   cp <service>/src/main/resources/application.properties.example \
      <service>/src/main/resources/application.properties
   ```

3. Run each service:
   ```bash
   cd <service> && ./mvnw spring-boot:run
   ```

## Configuration

Each service has an `application.properties.example` file in `src/main/resources/`. Copy it to `application.properties` and fill in the values. The actual `application.properties` is git-ignored.

> **Important:** `jwt.secret` must be the same value in all three services.

## Databases

Each service owns its own PostgreSQL database:

| Service | Database |
|---------|----------|
| auth-service | `flowforge_auth` |
| workflow-service | `flowforge_workflow` |
| execution-service | `flowforge_execution` |
