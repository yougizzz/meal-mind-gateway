# gateway-service

Spring Cloud Gateway for MealMind microservices. Handles JWT validation, Redis-backed rate limiting, CORS, and circuit breaking.

## Prerequisites

- JDK 21+
- Maven 3.9+
- Redis (local or remote)

## Getting Started

```bash
mvn spring-boot:run
```

Environment variables:

- `JWT_SECRET` – HMAC key used to validate incoming tokens.
- `SPRING_REDIS_HOST` / `SPRING_REDIS_PORT` – configure Redis connection if different from defaults.

## Docker

Build the image:

```bash
docker build -t gateway-service .
```

Run the container:

```bash
docker run --rm -p 8080:8080 \
  -e JWT_SECRET=your-secret \
  -e SPRING_REDIS_HOST=redis \
  gateway-service
```

## Rate Limiting

The `RateLimit` filter uses Redis to keep counts per client IP. Defaults can be tuned via:

- `gateway.rate-limit.window-seconds`

## Resilience

Resilience4j is configured with sensible defaults for all routes. Customize via `application.yml` as needed.

