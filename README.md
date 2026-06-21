# FlowForge

A distributed workflow automation platform inspired by Zapier, n8n, and Make.com. Users build trigger-action chains that execute asynchronously with retries, dead-letter queues, CRON scheduling, and real-time observability.

## Architecture

```
                        ┌─────────────────────────────────────┐
                        │          React Frontend              │
                        │   (Vite 5 · TypeScript · Tailwind)  │
                        └───────────────┬─────────────────────┘
                                        │ REST / JWT
               ┌────────────────────────┼────────────────────────┐
               ▼                        ▼                        ▼
       ┌──────────────┐        ┌──────────────────┐     ┌─────────────────┐
       │ auth-service │        │ workflow-service  │     │execution-service│
       │   :8081      │        │   :8082           │     │   :8083         │
       │              │        │                  │     │                 │
       │ - register   │        │ - workflow CRUD  │     │ - step executor │
       │ - login      │        │ - activate /     │     │ - retry + DLQ   │
       │ - refresh    │        │   deactivate     │     │ - analytics     │
       │ - JWT issue  │        │ - JobRunr CRON   │     │ - rate limiter  │
       └──────────────┘        │ - webhook trigger│     └────────┬────────┘
                               └────────┬─────────┘              │
                                        │                        │
                          ┌─────────────▼──────────────┐         │
                          │      Apache Kafka           │◄────────┘
                          │   topic: workflow.trigger   │  execution events
                          │   topic: execution.events   │
                          └─────────────────────────────┘
                                        │
               ┌────────────────────────┼────────────────────────┐
               ▼                        ▼                        ▼
       ┌──────────────┐        ┌──────────────────┐     ┌─────────────────┐
       │  PostgreSQL  │        │     Redis 7       │     │    JobRunr      │
       │  (3 DBs)     │        │                  │     │  (CRON engine)  │
       │              │        │ - workflow cache  │     │                 │
       │ flowforge_   │        │ - exec state      │     │ - dynamic job   │
       │   auth       │        │ - idempotency     │     │   registration  │
       │ flowforge_   │        │ - rate limiting   │     │ - PostgreSQL-   │
       │   workflow   │        └──────────────────┘     │   backed        │
       │ flowforge_   │                                  └─────────────────┘
       │   execution  │
       └──────────────┘
```

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.5 |
| Auth | Spring Security + JWT (access 15min / refresh 7d) |
| Database | PostgreSQL 16 |
| Messaging | Apache Kafka |
| Cache | Redis 7 |
| Scheduler | JobRunr 7.3.1 |
| Frontend | React 18 + Vite 5 + TypeScript + Tailwind + TanStack Query v5 |
| Containerisation | Docker + Docker Compose |

## Workflow Execution Flow

```
1. Trigger fires (WEBHOOK / SCHEDULE / MANUAL)
         │
         ▼
2. workflow-service publishes Kafka message
   { workflowId, userId, triggerType, correlationId }
         │
         ▼
3. execution-service consumer receives message
   ├── Gate 1: Redis SETNX on correlationId (24hr TTL) — dedup Kafka redeliveries
   └── Gate 2: Skip if workflow already RUNNING
         │
         ▼
4. Execution dispatched to thread pool (non-blocking consumer thread)
         │
         ▼
5. Steps run in order via ActionHandler registry
   ├── HTTP_REQUEST  — outbound HTTP call
   ├── EMAIL         — JavaMailSender (optional SMTP config)
   ├── SLACK_WEBHOOK — POST to Slack incoming webhook
   ├── DATABASE_INSERT — PreparedStatement against caller-supplied JDBC URL
   ├── DELAY         — configurable sleep between steps
   └── CONDITION     — if/else branching; false condition exits early as COMPLETED
         │
         ▼
6. Step failure → exponential backoff retry (5 × 2^attempt seconds, default 3 retries)
         │
         ▼
7. Max retries exhausted → DLQ (dead_letter_queue table) → Execution FAILED
         │
         ▼
8. Manual replay: POST /api/v1/executions/{id}/retry
   Resets failed steps, reruns from first failure point
```

## Key Design Decisions

| Decision | Why |
|----------|-----|
| **JobRunr over Quartz** | Quartz has a dated API; JobRunr supports dynamic job registration at runtime (needed for per-workflow CRON) |
| **JobRunr over ShedLock** | ShedLock only prevents concurrent execution — it can't register/cancel individual jobs dynamically |
| **X-Service-Key internal endpoint** | Kafka-triggered flows have no SecurityContext/JWT; sharing a secret header avoids passing tokens through messages |
| **AsyncExecutionRunner** | Kafka consumer thread blocks the partition while running; async dispatch lets the consumer return in <1ms while executions run on a dedicated thread pool |
| **MDC propagation to async threads** | MDC is thread-local; capturing it before dispatch and restoring it inside the lambda keeps correlationId/workflowId/executionId in every log line across thread handoffs |
| **Redis SETNX idempotency** | Kafka guarantees at-least-once delivery; a 24hr dedup window on correlationId prevents duplicate executions on redelivery |
| **Two-layer idempotency** | Redis dedup (Gate 1) + RUNNING status check (Gate 2) covers both Kafka redeliveries and concurrent API triggers |

## Quick Start

### Docker Compose (recommended)

```bash
cd backend
docker-compose up --build
```

All services, infrastructure, and databases start together.

### Local Development

1. Start infrastructure:
   ```bash
   cd backend
   docker-compose up postgres kafka zookeeper redis -d
   ```

2. Each service reads `application.properties` which falls back to `localhost` defaults — no manual configuration needed for local dev.

3. Run each service:
   ```bash
   cd backend/auth-service     && ./mvnw spring-boot:run
   cd backend/workflow-service && ./mvnw spring-boot:run
   cd backend/execution-service && ./mvnw spring-boot:run
   ```

4. Start the frontend:
   ```bash
   cd frontend && npm install && npm run dev
   ```

### Environment Variables (production / docker-compose override)

| Variable | Used by | Description |
|----------|---------|-------------|
| `JWT_SECRET` | all services | Must be identical across all three; min 32 chars |
| `INTERNAL_SERVICE_KEY` | workflow + execution | Shared secret for service-to-service calls |
| `DB_URL` | all services | JDBC URL for the service's PostgreSQL database |
| `DB_USERNAME` / `DB_PASSWORD` | all services | Database credentials |
| `KAFKA_BOOTSTRAP_SERVERS` | workflow + execution | e.g. `kafka:9092` in Docker |
| `REDIS_HOST` / `REDIS_PORT` | execution | Redis connection |
| `WORKFLOW_SERVICE_URL` | execution | Base URL for workflow-service internal calls |

## API Overview

### auth-service `:8081`
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/auth/register` | Register new user |
| POST | `/api/v1/auth/login` | Login, returns JWT + refresh token |
| POST | `/api/v1/auth/refresh` | Exchange refresh token for new access token |

### workflow-service `:8082`
| Method | Path | Description |
|--------|------|-------------|
| POST | `/api/v1/workflows` | Create workflow |
| GET | `/api/v1/workflows` | List user's workflows |
| GET | `/api/v1/workflows/{id}` | Get workflow |
| PUT | `/api/v1/workflows/{id}` | Update workflow |
| DELETE | `/api/v1/workflows/{id}` | Delete workflow |
| POST | `/api/v1/workflows/{id}/activate` | Activate (registers CRON if SCHEDULE trigger) |
| POST | `/api/v1/workflows/{id}/deactivate` | Deactivate (removes CRON job) |
| POST | `/api/v1/workflows/{id}/trigger` | Manual trigger |
| POST | `/api/v1/webhooks/{workflowId}` | Webhook trigger (public) |

### execution-service `:8083`
| Method | Path | Description |
|--------|------|-------------|
| GET | `/api/v1/executions/{id}` | Get execution details + steps |
| GET | `/api/v1/executions/workflow/{workflowId}` | All executions for a workflow |
| POST | `/api/v1/executions/{id}/retry` | Retry from first failed step |
| GET | `/api/v1/executions/analytics` | Summary stats (total, success rate, DLQ count) |
| GET | `/api/v1/executions/dlq` | Dead letter queue entries |

## Redis Key Patterns

| Key | TTL | Purpose |
|-----|-----|---------|
| `workflow:cache:{id}` | 5 min | Cached workflow definition (avoids repeated REST calls) |
| `execution:state:{id}` | 1 hr | Live execution status for polling |
| `idempotency:{correlationId}` | 24 hr | Kafka dedup — prevents processing same message twice |
| `rate:limit:{userId}` | 60 sec | Rate limiter — max 10 triggers/min per user |

## Project Structure

```
flowforge/
├── frontend/               # React + Vite frontend
│   └── src/
│       ├── pages/          # WorkflowsPage, ExecutionsPage, DashboardPage, DLQPage
│       ├── api/            # Axios clients per service
│       └── components/
└── backend/
    ├── docker-compose.yml
    ├── auth-service/       # :8081 — JWT auth
    ├── workflow-service/   # :8082 — workflow CRUD + CRON scheduling
    └── execution-service/  # :8083 — async execution engine
```
