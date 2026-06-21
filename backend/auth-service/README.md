# Auth Service

Handles user registration, login, and JWT-based authentication for FlowForge.

## Tech Stack

- Java 21 / Spring Boot 3.5
- PostgreSQL (`flowforge_auth` database)
- JWT (access + refresh tokens)

## Port

`8081`

## API Endpoints

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/api/v1/auth/register` | Public | Register a new user |
| POST | `/api/v1/auth/login` | Public | Login and receive JWT tokens |
| POST | `/api/v1/auth/refreshToken` | Public | Exchange refresh token for new access token |
| GET | `/api/v1/user/me` | Bearer token | Get current authenticated user |

## Local Setup

1. Create a PostgreSQL database named `flowforge_auth`.

2. Copy the example config and fill in your values:
   ```bash
   cp src/main/resources/application.properties.example src/main/resources/application.properties
   ```

3. Run the service:
   ```bash
   ./mvnw spring-boot:run
   ```

> **Note:** The JWT secret must be the same across all FlowForge services.

## Docker

Built and orchestrated via the root `docker-compose.yml`. The service depends on the `postgres` container being healthy before starting.
