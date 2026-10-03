# Spring Boot Rate Limit Service

A Spring Boot HTTP service that applies a Redis-backed, per-client-IP request limit using Bucket4j and Lettuce.

## How It Works

For each request, `RateLimitFilter` determines the client IP and asks `RateLimitingService` for the corresponding Bucket4j bucket. Buckets are stored through Bucket4j's Lettuce-based Redis proxy manager, so the same rate-limit state is shared by application instances using the same Redis database.

Each IP receives a bucket with a capacity of 10 tokens. The bucket refills by 10 tokens every minute. Each request consumes one token:

- If a token is available, the request continues and the response includes `X-Rate-Limit-Remaining`.
- If no token is available, the service returns HTTP `429 Too Many Requests`, a JSON error body, and `X-Rate-Limit-Retry-After-Seconds`.

The filter uses the first value in `X-Forwarded-For` when that header is present; otherwise, it uses the servlet request's remote address. Only trust forwarded client-IP headers when requests come through a proxy that you control and configure to set the header.

Bucket records use an expiration strategy based on the time needed to refill a bucket up to its maximum, with the client-side configuration setting a one-hour duration.

## HTTP Endpoint

| Method | Path                | Access | Response                                |
| ------ | ------------------- | ------ | --------------------------------------- |
| `GET`  | `/api/public/hello` | Public | `Hello! This is the rate limit service` |

The rate-limit filter is registered before Spring Security's username/password authentication filter. `/api/public/**` is permitted without authentication; other routes require authentication. This project does not define a custom user store or authentication provider. CSRF protection is disabled in the configured security chain.

## Requirements

- Java 21
- Maven (or the included Maven Wrapper)
- A reachable Redis server

Redis connection settings are read from these required Spring properties, mapped in `src/main/resources/application.yml` to environment variables:

| Environment variable | Purpose        |
| -------------------- | -------------- |
| `REDIS_HOST`         | Redis hostname |
| `REDIS_PORT`         | Redis port     |
| `REDIS_USERNAME`     | Redis username |
| `REDIS_PASSWORD`     | Redis password |

The application optionally loads a `.env` file from its working directory using `java-dotenv`. Existing operating-system environment variables take precedence over values loaded from that file. Keep credentials out of version control and never commit a populated `.env` file.

The current Redis client configuration creates a host-and-port connection with username/password authentication. It does not explicitly enable TLS; configure TLS if required by your Redis provider before deploying against a TLS-only endpoint.

## Run Locally

Provide the four Redis environment variables in your shell, or provide them in a local `.env` file in the project's working directory. Then run:

```sh
./mvnw spring-boot:run
```

By default, Spring Boot serves on port `8080`. Try the public endpoint with:

```sh
curl -i http://localhost:8080/api/public/hello
```

The rate-limit filter needs Redis while the application initializes its Redis proxy manager. Redis DNS/network reachability and compatible credentials must therefore be in place for startup; a Redis connection failure can prevent the web server from starting.

## Build and Test

```sh
./mvnw test
./mvnw package
```

The context-load test starts the Spring application context, so it also requires a reachable Redis server and valid connection settings.

## Project Structure

- `config/RateLimitConfig.java` creates the Lettuce Redis client and Bucket4j proxy manager.
- `config/SecurityConfig.java` configures the Spring Security filter chain and registers the rate-limit filter.
- `filter/RateLimitFilter.java` identifies the client IP, consumes a token, and handles allowed or throttled responses.
- `service/RateLimitingService.java` defines per-IP bucket lookup and the 10-requests-per-minute bucket policy.
- `controller/PublicController.java` implements the public hello endpoint.
- `EnvLoader.java` optionally loads local dotenv values into system properties before Spring starts.

## Troubleshooting

- **Unresolved Redis hostname:** verify DNS resolution for `REDIS_HOST` and check VPN, firewall, and outbound access to `REDIS_PORT`. A Netty warning about the missing macOS native DNS resolver is separate from Redis authentication and can indicate that the fallback resolver may behave differently on macOS.
- **Connection refused or timeout:** verify the provider's host and port, network allow-lists, and that the Redis service is running and reachable.
- **Authentication error:** verify both username and password. Authentication can only be checked after the host is resolved and a connection is established.
- **TLS-required endpoint:** the current client configuration does not explicitly enable TLS, so use a URI/client configuration that matches the Redis provider's TLS requirements.
- **Tests fail during context startup:** the current context-load test constructs the Redis proxy manager; run tests with a reachable Redis instance or add a test-specific Redis substitute/configuration.
