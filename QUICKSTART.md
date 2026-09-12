# Run the jOOQ / Dapper comparison

The ZIP contains the complete modified source project, benchmark scripts, and
prebuilt applications for the five implementations below. No compilation is
needed to run the included artifacts. Do not move the Quarkus runner JARs or the
ASP.NET DLL away from their accompanying dependencies.

## Prerequisites

Use macOS or Linux with:

- Java 21 or newer (also needed by the JBang load generator).
- .NET 10 SDK, or the .NET 10 ASP.NET Core runtime, for the ASP.NET application.
- Docker or Podman running, with enough memory for PostgreSQL and Grafana LGTM.
- JBang, curl, lsof, bc, and the standard shell utilities.
- On macOS, GNU coreutils on PATH (including `realpath`, `timeout`, and `gdate`).

Network access is required to pull the database/observability containers and
download the JBang/Hyperfoil load generator on first use. The ZIP does not contain
runtime installations, container images, or dependency caches.

The existing stress harness uses port 8080 and starts/stops containers named
`fruits_db` and `otel_lgtm`. Run tests one at a time on a machine where those ports
and container names are available. As in the original script, a process already
listening on port 8080 may be terminated.

## Run

Extract the ZIP and change into `spring-quarkus-jooq-dapper`. From that directory:

```sh
./stress.sh springboot3/target/springboot3.jar
./stress.sh springboot4/target/springboot4.jar
./stress.sh quarkus3/target/quarkus-app/quarkus-run.jar
./stress.sh quarkus3-virtual/target/quarkus-app/quarkus-run.jar
./stress.sh aspnet10/target/aspnet10.dll
```

Each invocation starts the shared PostgreSQL and observability infrastructure,
runs the `/fruits` workload, prints throughput, time to first request, and RSS,
then stops the infrastructure. The root script delegates to `scripts/stress.sh`.
The original convention also works:

```sh
cd scripts
./stress.sh ../aspnet10/target/aspnet10.dll
```

Java runs with the original `-XX:ActiveProcessorCount=4`, Parallel GC, and 512 MiB
initial/maximum Java heap. .NET runs with `DOTNET_PROCESSOR_COUNT=4`, using its
normal GC and memory settings. Processor-count hints do not isolate CPUs and the
Java heap limit is not a cross-runtime total-memory limit. These quick laptop
runs are not controlled performance measurements; use `scripts/perf-lab` for that.

To inspect a command without launching an application or touching containers:

```sh
./stress.sh --dry-run aspnet10/target/aspnet10.dll
```

## Rebuild after editing

Java (from the project root):

```sh
./mvnw -pl springboot3,springboot4,quarkus3,quarkus3-virtual -DskipTests clean package
```

ASP.NET (requires the .NET 10 SDK):

```sh
dotnet publish aspnet10/src/AspNet10.csproj -c Release -o aspnet10/target
```

The legacy `quarkus3-spring-compatibility` source is included for completeness,
but it still uses ORM persistence and has no prebuilt artifact in this ZIP.

## Verification

Java package builds and Spring AOT builds were checked, as were .NET publishing,
application startup, and the .NET HTTP tests. The stress-script command routing
and archive contents were checked. Full container-backed stress runs and
database integration tests have not been run in the packaging environment.
