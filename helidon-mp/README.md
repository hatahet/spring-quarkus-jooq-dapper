# Helidon MP

Helidon 4.5.4 on Java 21+, using jOOQ 3.19.22 and PostgreSQL. The API, domain model, ordered joins, sequence-backed inserts, and seed data match the [benchmark TCK](../TCK.md).

Both Helidon implementations use virtual threads and HikariCP pools with a maximum of **50 connections**. Reads borrow a connection for each jOOQ query; inserts use an explicit local jOOQ transaction. Connections return to the pool when each operation finishes. The pool closes on application shutdown.

## Build and run

From the repository root:

```sh
./mvnw -f helidon-mp/pom.xml clean verify
./scripts/infra.sh -s
java -jar helidon-mp/target/helidon-mp.jar
```

Keep `target/libs/` beside the application JAR. `verify` runs HTTP tests with a mocked repository and repository tests against an isolated PostgreSQL 17 Testcontainer, so Docker is required. To package without running tests, use `-DskipTests package`.

Configuration is in `src/main/resources/application.yaml`. Defaults are port 8080 and `jdbc:postgresql://localhost:5432/fruits`, with username/password `fruits`. System properties can override configuration, for example:

```sh
java -Dserver.port=8081 -Djavax.sql.DataSource.fruits.dataSource.url=jdbc:postgresql://localhost:5433/fruits -jar helidon-mp/target/helidon-mp.jar
```

After packaging both modules, run the optional smoke check against their actual JARs:

```sh
python3 scripts/tests/helidon-smoke-test.py
```

It starts an isolated PostgreSQL container and local OTLP receiver, checks the API, health, metrics, exported service/JDBC spans, the 50-connection limit, and pool shutdown. Add `helidon-se` or `helidon-mp` to check just one module. This check forces full trace sampling only for the test; normal benchmark runs use 10%.

## Endpoints and observability

- `GET /fruits`, `GET /fruits/{name}`, `POST /fruits`
- Database readiness: `/health/ready`
- Prometheus metrics: `/metrics` (request `Accept: text/plain`)
- HTTP, service, and JDBC tracing exported to OTLP on `localhost:4318`, with 10% sampling

## Benchmark

```sh
./stress.sh helidon-mp/target/helidon-mp.jar
cd scripts/perf-lab
./run-benchmarks.sh --host LOCAL --runtimes helidon-se,helidon-mp --output-dir ./results
```

For comparisons using the same thread model, select Spring and Quarkus virtual-thread runtimes alongside Helidon. The Helidon runtime entries use the normal JVM memory, CPU isolation, profiling, startup, RSS, and throughput measurement settings. Only JVM builds are configured for Helidon.

Helidon manages the HikariCP datasource through its CDI integration. `@Named("fruits")` supplies the pool to the jOOQ producer and database health check. Pool sizing is configured by `javax.sql.DataSource.fruits.maximumPoolSize`.
