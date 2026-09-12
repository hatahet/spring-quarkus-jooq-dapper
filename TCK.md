# Framework Performance Benchmarking TCK

## 1. Overview

This document defines the requirements for implementations in the performance benchmarking suite. The goal is a like-for-like comparison across frameworks and runtimes by keeping the HTTP contract, PostgreSQL schema, seed data, logical SQL workload, object graph, and observability behavior equivalent.

The application models fruits sold by stores with a per-store price.

### Compliance levels

- **MUST** — required for a valid comparison
- **SHOULD** — strongly recommended; deviations need a documented reason
- **MAY** — part of the framework-specific adaptation surface

### Comparison set

| Module | Runtime | Data access |
|---|---|---|
| `springboot3` | Spring Boot 3 / JVM | jOOQ |
| `springboot4` | Spring Boot 4 / JVM | jOOQ |
| `quarkus3` | Quarkus 3 / JVM or native | jOOQ |
| `quarkus3-virtual` | Quarkus 3 / JVM virtual threads | jOOQ |
| `aspnet10` | ASP.NET Core / .NET 10 | Dapper |

`quarkus3` is the Java reference implementation. `aspnet10` is the .NET reference implementation. The `quarkus3-spring-compatibility` module is a legacy ORM-based compatibility example and is excluded from this comparison set.

## 2. Layering and dependency rules

Every implementation MUST contain these conceptual layers. Names MAY follow language conventions.

| Layer | Java package | .NET namespace | May depend on |
|---|---|---|---|
| Domain | `org.acme.domain` | `AspNet10.Domain` | no application layer |
| DTO | `org.acme.dto` | `AspNet10.Dto` | no application layer |
| Mapping | `org.acme.mapping` | `AspNet10.Mapping` | domain, DTO |
| Repository | `org.acme.repository` | `AspNet10.Repository` | domain, SQL client |
| Service | `org.acme.service` | `AspNet10.Service` | repository, mapping, domain, DTO |
| HTTP | `org.acme.rest` | `AspNet10.Controllers` | service, DTO |
| Configuration | `org.acme.config` or resources | application root | unrestricted framework wiring |

The HTTP layer MUST NOT access the repository directly. The repository MUST NOT expose DTOs. Domain classes MUST be plain objects without ORM mappings.

## 3. Domain model

Implementations MUST represent the same fields and relationships. Java and .NET type names MAY differ where their semantics are equivalent.

### Address

| Field | Logical type | Validation |
|---|---|---|
| address/street | string | not blank |
| city | string | not blank |
| country | string | not blank |

### Fruit

| Field | Logical type | Notes |
|---|---|---|
| id | nullable 64-bit integer | assigned by `fruits_seq` |
| name | string | not blank, unique in the database |
| description | nullable string | |
| storePrices | collection of `StoreFruitPrice` | empty when no prices exist |

### Store

| Field | Logical type | Notes |
|---|---|---|
| id | 64-bit integer | assigned by `stores_seq` |
| name | string | not blank, unique in the database |
| currency | string | not blank |
| address | `Address` | required |

### StoreFruitPriceId

Composite identifier containing `storeId` and `fruitId`, both 64-bit integers.

### StoreFruitPrice

| Field | Logical type | Notes |
|---|---|---|
| id | `StoreFruitPriceId` | composite identifier |
| store | `Store` | required |
| fruit | `Fruit` | required back-reference; never serialized directly |
| price | decimal | `numeric(12,2)`, value must be at least zero |

## 4. DTO and JSON contract

DTOs MUST expose this logical shape:

- `AddressDTO(address, city, country)`
- `StoreDTO(id, name, currency, address)`
- `StoreFruitPriceDTO(store, price)` where `price` serializes as a JSON number
- `FruitDTO(id, name, description, storePrices)`

`name` MUST be validated as not blank on POST. Nested required values and non-negative prices SHOULD use the framework's normal validation mechanism.

Empty or null optional values MUST be omitted from JSON. A fruit with prices has this shape:

```json
{
  "id": 1,
  "name": "Apple",
  "description": "Hearty fruit",
  "storePrices": [
    {
      "store": {
        "id": 1,
        "name": "Store 1",
        "currency": "USD",
        "address": {
          "address": "123 Main St",
          "city": "Anytown",
          "country": "USA"
        }
      },
      "price": 1.29
    }
  ]
}
```

The complete contract is defined by [`openapi.yml`](openapi.yml).

## 5. REST API

Base path: `/fruits`

| Method | Path | Request | Success | Failure |
|---|---|---|---|---|
| `GET` | `/fruits` | none | `200`, array of fruits | — |
| `GET` | `/fruits/{name}` | none | `200`, one fruit | `404`, empty body |
| `POST` | `/fruits` | validated fruit JSON | `200`, saved fruit | framework-standard validation response |

The controller MUST depend only on the service and MUST delegate business logic to it. JAX-RS, Spring MVC, and ASP.NET Core MVC annotations and response wrappers MAY differ.

## 6. Service contract

The service MUST provide these semantic operations; synchronous or asynchronous signatures MAY follow the runtime's conventions.

| Operation | Java reference | .NET reference | Transaction |
|---|---|---|---|
| List fruits | `List<FruitDTO> getAllFruits()` | `Task<IReadOnlyList<FruitDto>> GetAllFruitsAsync(...)` | read-only / SUPPORTS |
| Find by name | `Optional<FruitDTO> getFruitByName(String)` | `Task<FruitDto?> GetFruitByNameAsync(...)` | read-only / SUPPORTS |
| Create fruit | `FruitDTO createFruit(FruitDTO)` | `Task<FruitDto> CreateFruitAsync(...)` | required or explicit local transaction |

The service MUST map between domain objects and DTOs and MUST emit these spans:

| Operation | Span name | Attribute |
|---|---|---|
| List | `FruitService.getAllFruits` | — |
| Find | `FruitService.getFruitByName` | `arg.name` |
| Create | `FruitService.createFruit` | `arg.fruit` |

Annotations, interceptors, and direct OpenTelemetry API instrumentation are all acceptable when they produce equivalent spans.

## 7. Repository and SQL workload

The repository MUST provide find-all, find-by-name, and save operations. Java implementations MUST use jOOQ. The .NET implementation MUST use Dapper with Npgsql.

### Read query

Both read operations MUST use one query with this join and projection. The find-by-name variant adds `WHERE f.name = ?` (or a named bind parameter). A DSL-generated equivalent is acceptable.

```sql
SELECT f.id,
       f.name,
       f.description,
       s.id,
       s.name,
       s.currency,
       s.address,
       s.city,
       s.country,
       sfp.price
FROM fruits f
LEFT JOIN store_fruit_prices sfp ON sfp.fruit_id = f.id
LEFT JOIN stores s ON s.id = sfp.store_id
ORDER BY f.id, s.id
```

Repository mapping MUST:

- create one `Fruit` per distinct fruit ID;
- preserve ascending fruit and store order;
- add one `StoreFruitPrice` for each joined store row;
- retain fruits with no prices and give them an empty price collection;
- build the store address and composite price identifier; and
- set the price's fruit back-reference without serializing it.

### Insert query

New fruits MUST be inserted with the PostgreSQL sequence and returned ID:

```sql
INSERT INTO fruits (id, name, description)
VALUES (nextval('fruits_seq'), ?, ?)
RETURNING id
```

The returned ID MUST be placed on the saved domain object. SQL values MUST be bound as parameters.

## 8. Data and schema

| Table | Columns | Primary key / sequence |
|---|---|---|
| `fruits` | `id bigint`, `name varchar not null unique`, `description varchar` | `id`, `fruits_seq` |
| `stores` | `id bigint`, `name varchar not null unique`, `currency varchar not null`, `address varchar not null`, `city varchar not null`, `country varchar not null` | `id`, `stores_seq` |
| `store_fruit_prices` | `store_id bigint`, `fruit_id bigint`, `price numeric(12,2) not null` | composite `(fruit_id, store_id)` plus foreign keys |

Every test environment MUST apply an explicit schema equivalent to `schema.sql`; no ORM schema generation is part of the comparison.

Seed data MUST match [`scripts/dbdata/db.sql`](scripts/dbdata/db.sql):

- 10 fruits with IDs 1–10;
- 8 stores with IDs 1–8;
- 34 store/fruit price rows;
- `fruits_seq` restarted at 11 and `stores_seq` at 9.

Framework-specific seed filenames such as `data.sql` and `import.sql` MAY differ, but their data MUST be equivalent.

## 9. Runtime configuration

| Concern | Required value |
|---|---|
| Production database | PostgreSQL at `localhost:5432/fruits`, username/password `fruits` |
| Connection pool | target maximum of approximately 50 connections |
| JSON | camel-case API properties; omit empty/null optional fields |
| Trace sampling | 10% (`0.1`) |
| Database tracing | JDBC or Npgsql calls instrumented |
| Health | database-aware health endpoint exposed |
| Metrics | Prometheus-compatible endpoint exposed |
| HTTP port | `8080` |

Configuration format, server implementation, dependency injection, pool implementation, and test database provisioning MAY vary. JVM-only settings such as garbage collection, virtual threads, AOT, and native image configuration apply only to the corresponding Java runtime.

## 10. Testing

### Repository tests

Repository integration tests MUST use a real PostgreSQL instance provided by Dev Services, Testcontainers, or an equivalent isolated mechanism. At minimum they MUST:

- read `Apple` and validate its joined store-price graph;
- save `Grapefruit` with description `Summer fruit`;
- assert the generated ID is non-null and follows the seeded sequence; and
- find the newly saved fruit by name.

Tests SHOULD roll back or use an isolated database/container.

### Controller tests

Controller tests MUST replace or mock the repository and exercise HTTP routing, serialization, and response codes. They MUST cover:

- listing a mapped fruit with nested store/address/price data;
- finding an existing fruit;
- receiving `404` for an unknown fruit; and
- posting a fruit and receiving its generated ID, name, and description.

Mockito, NSubstitute, REST Assured, MockMvc, `WebApplicationFactory`, and equivalent framework-native tools are allowed.

### End-to-end tests

Full-stack tests are optional. When present, they MUST initialize the same schema and 10-fruit seed dataset before assertions.

## 11. Framework adaptation surface

The following MAY vary without invalidating parity:

- language and naming conventions;
- annotations and dependency-injection lifecycle;
- synchronous Java APIs versus asynchronous .NET APIs;
- jOOQ DSL versus parameterized Dapper SQL;
- transaction annotation or explicit transaction APIs;
- hand-written mapping implementation;
- health and metrics route names;
- native-image, AOT, and virtual-thread support; and
- framework-specific test harnesses.

The following MUST NOT vary:

- endpoint paths, JSON fields, response status behavior;
- PostgreSQL schema and seed data;
- read query join shape, ordering, and resulting domain graph;
- sequence-backed insert behavior;
- service span names and 10% sampling; and
- the logical controller/service/repository separation.

## 12. Compliance checklist

- [ ] Same three `/fruits` operations and OpenAPI-compatible JSON
- [ ] Plain domain objects with the fields and relationships in Section 3
- [ ] Controller depends only on service; repository returns domain objects
- [ ] Java uses jOOQ; .NET uses Dapper/Npgsql
- [ ] One ordered `LEFT JOIN` query reconstructs the complete graph
- [ ] Inserts use `fruits_seq` and `RETURNING id`
- [ ] Explicit schema plus identical 10-fruit seed data
- [ ] Equivalent service spans, database tracing, health, and metrics
- [ ] Controller tests plus real-PostgreSQL repository tests
