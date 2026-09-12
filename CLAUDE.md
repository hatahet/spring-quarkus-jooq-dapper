# CLAUDE.md

## Project Overview

Performance benchmarking suite comparing Spring Boot, Quarkus, and ASP.NET Core. The goal is like-for-like comparisons using equivalent domain models, REST endpoints, PostgreSQL data, and SQL access patterns.

## Modules

| Module | Framework | Notes |
|--------|-----------|-------|
| `springboot3/` | Spring Boot 3.x | Tomcat, jOOQ |
| `springboot4/` | Spring Boot 4.x | Tomcat, jOOQ |
| `quarkus3/` | Quarkus 3.x | Quarkus REST, jOOQ |
| `quarkus3-virtual/` | Quarkus 3.x | jOOQ, virtual threads variant |
| `quarkus3-spring-compatibility/` | Quarkus 3.x | Legacy ORM-based Spring compatibility variant |
| `aspnet10/` | ASP.NET Core, .NET 10 | Dapper, Npgsql |

All comparison modules share the same Fruit/Store/Address domain, DTO shape, and REST contract.

## Build

```sh
./mvnw clean verify          # Build all Java modules
./mvnw clean verify -pl quarkus3  # Build single module
dotnet test aspnet10/AspNet10.slnx # Build and test ASP.NET Core
```

- Java 21 required
- Maven wrapper included (`./mvnw`)
- Parent POM is an aggregator only (no shared dependencies)
- Each module manages its own dependencies independently

## Key Technologies

- **Quarkus modules**: jOOQ, Quarkus REST Jackson, SmallRye Health, Micrometer OpenTelemetry, PostgreSQL JDBC, config via YAML
- **Spring modules**: jOOQ, Spring Web (MVC), Spring Actuator, OpenTelemetry Spring Boot starter, PostgreSQL
- **ASP.NET Core**: Dapper, Npgsql, ASP.NET Core health checks, OpenTelemetry
- **Testing**: JUnit/AssertJ/REST Assured for Java; xUnit/NSubstitute for .NET; Testcontainers for repository integration
- **Native image**: GraalVM support in both Quarkus (`-Pnative`) and Spring (`native-maven-plugin`)

## Infrastructure

PostgreSQL on `localhost:5432`. Managed via scripts:

```sh
cd scripts
./infra.sh -s   # Start DB (Docker/Podman), create tables, seed data
./infra.sh -d   # Stop DB
```

## Benchmarking Scripts

Located in `scripts/`:
- `stress.sh` - Throughput measurement using Hyperfoil (wrk2 bindings)
- `1strequest.sh` - Startup time and RSS measurement
- `run-requests.sh` - Run requests against a running app
- `perf-lab/run-benchmarks.sh` - Full benchmarking automation (used in CI/perf labs)

## Branching Strategy

- `main` - Tuned strategy (reasonable perf tuning allowed)
- `ootb` - Out-of-the-box strategy (no tuning allowed)

## Conventions

- Application code must maintain parity across all modules (same domain, same endpoints, same behavior)
- Changes to architecture (e.g., virtual threads) must be applied to all modules
- Config files use YAML (`application.yml`) in all modules
- `.gitignore` excludes `target/`, IDE files, and `.claude/`

## Things to keep in mind

- When doing `sudo` in qDup scripts, ensure you use the `sudo` script whenever possible rather than doing `- sh: sudo...`
    - The `sudo` script takes a `command` argument
