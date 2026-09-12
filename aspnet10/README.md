# ASP.NET Core 10 + Dapper

This module implements the same `/fruits` API and PostgreSQL workload as the Java modules using ASP.NET Core 10, Dapper, and Npgsql.

## Build and test

```shell
dotnet test AspNet10.slnx
```

The repository integration test uses Testcontainers and therefore requires Docker or Podman. To run only tests that do not need a container:

```shell
dotnet test AspNet10.slnx --filter "Category!=Integration"
```

## Run

Start the shared infrastructure from the repository root, then launch the application:

```shell
scripts/infra.sh -s
dotnet run --project aspnet10/src/AspNet10.csproj -c Release
```

The service listens on port `8080`. Override its database settings with standard ASP.NET Core configuration such as `ConnectionStrings__Fruits`.

Health and Prometheus metrics are exposed at `/health` and `/metrics`. To include this runtime in the automated comparison, pass `--runtimes aspnet10` to `scripts/perf-lab/run-benchmarks.sh` (it is also part of the default runtime set).

For the existing single-machine stress script, publish first and pass the DLL:

```shell
dotnet publish aspnet10/src/AspNet10.csproj -c Release -o aspnet10/target
./stress.sh aspnet10/target/aspnet10.dll
```

Run these commands from the repository root. The downloadable ZIP already
contains the published artifacts; see `QUICKSTART.md`.
