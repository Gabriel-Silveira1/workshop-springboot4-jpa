# coursejava

A study-oriented Spring Boot REST API that models a small e-commerce domain (users, categories, products, orders, order items and payments). The project is organized in a classic layered architecture and is intended as a hands-on reference for building, running and extending a Spring Boot application backed by JPA/Hibernate.

---

## Table of Contents

1. [Tech Stack](#1-tech-stack)
2. [Architecture](#2-architecture)
3. [Domain Model](#3-domain-model)
4. [Project Structure](#4-project-structure)
5. [Prerequisites](#5-prerequisites)
6. [Configuration & Profiles](#6-configuration--profiles)
7. [Running the Application](#7-running-the-application)
8. [REST API](#8-rest-api)
9. [Error Handling](#9-error-handling)
10. [Database Seeding](#10-database-seeding)
11. [Tests](#11-tests)
12. [Roadmap / Possible Improvements](#12-roadmap--possible-improvements)

---

## 1. Tech Stack

| Layer            | Technology                                  |
|------------------|---------------------------------------------|
| Language         | Java 25                                     |
| Framework        | Spring Boot 4.0.5 (Web MVC, Data JPA)       |
| Persistence      | Hibernate / Jakarta Persistence (JPA)       |
| Database (dev)   | PostgreSQL                                  |
| Database (test)  | H2 (in-memory) + H2 Console                 |
| Build Tool       | Maven (Maven Wrapper included)              |
| JSON             | Jackson                                     |

Key dependencies are declared in [pom.xml](pom.xml).

---

## 2. Architecture

The codebase follows a standard three-tier layered architecture, keeping each layer focused on a single responsibility:

```
HTTP Request
     |
     v
+------------------+    delegates    +-----------------+    persists    +-------------------+
| Resource (REST)  | --------------> | Service (rules) | -------------> | Repository (JPA)  |
+------------------+                 +-----------------+                +-------------------+
        ^                                                                         |
        |                                                                         v
        +-------------- @ControllerAdvice ←--- domain exception ---           Database
```

- **Resource** — `@RestController` classes under `resource/`. They expose HTTP endpoints, parse input and return `ResponseEntity`. They do **not** contain business logic.
- **Service** — `@Component` classes under `services/`. They orchestrate business operations and translate persistence errors into domain exceptions.
- **Repository** — Spring Data JPA interfaces under `repositories/`, extending `JpaRepository`.
- **Domain exceptions** — `services/exceptions/` (`ResourceNotFoundException`, `DataBaseException`) are translated to HTTP responses by `resource/exceptions/ResourceExceptionHandler` (`@ControllerAdvice`).

---

## 3. Domain Model

The domain represents an e-commerce flow where a `User` (client) places `Order`s, each composed of one or more `OrderItem`s referencing a `Product`. A `Product` belongs to one or more `Category`s. An `Order` may have an associated `Payment`.

```
   User 1 ---* Order 1 ---* OrderItem *--- 1 Product *---* Category
                  |
                  | 1
                  v
               Payment
```

### Entities

| Entity        | File                                                         | Notes                                                                   |
|---------------|--------------------------------------------------------------|-------------------------------------------------------------------------|
| `User`        | [User.java](src/main/java/com/coursejava/coursejava/entities/User.java) | Client of the system; owns a list of orders.                            |
| `Order`       | [Order.java](src/main/java/com/coursejava/coursejava/entities/Order.java) | Holds `moment`, `OrderStatus`, client reference, items and payment.     |
| `OrderItem`   | [OrderItem.java](src/main/java/com/coursejava/coursejava/entities/OrderItem.java) | Uses an `@EmbeddedId` composite key (`OrderItemPk`) of order + product. |
| `OrderItemPk` | [OrderItemPk.java](src/main/java/com/coursejava/coursejava/entities/pk/OrderItemPk.java) | Composite primary key for `OrderItem`.                                  |
| `Product`     | [Product.java](src/main/java/com/coursejava/coursejava/entities/Product.java) | Many-to-many with `Category` via `tb_product_category`.                 |
| `Category`    | [Category.java](src/main/java/com/coursejava/coursejava/entities/Category.java) | Inverse side of the product/category relation.                          |
| `Payment`     | [Payment.java](src/main/java/com/coursejava/coursejava/entities/Payment.java) | One-to-one with `Order`, sharing primary key via `@MapsId`.             |
| `OrderStatus` | [OrderStatus.java](src/main/java/com/coursejava/coursejava/entities/enums/OrderStatus.java) | Enum: `WAITING_PAYMENT`, `PAID`, `SHIPPED`, `DELIVERED`, `CANCELED`.    |

---

## 4. Project Structure

```
src/
└── main/
    ├── java/com/coursejava/coursejava/
    │   ├── CoursejavaApplication.java          # Spring Boot entry point
    │   ├── config/
    │   │   └── TestConfig.java                 # Seed data (profile: test)
    │   ├── entities/
    │   │   ├── User.java
    │   │   ├── Order.java
    │   │   ├── OrderItem.java
    │   │   ├── Product.java
    │   │   ├── Category.java
    │   │   ├── Payment.java
    │   │   ├── enums/OrderStatus.java
    │   │   └── pk/OrderItemPk.java
    │   ├── repositories/                       # Spring Data JPA interfaces
    │   ├── services/                           # Business orchestration
    │   │   └── exceptions/                     # Domain exceptions
    │   └── resource/                           # REST controllers
    │       └── exceptions/                     # ControllerAdvice + StandardError
    └── resources/
        ├── application.properties              # Active profile, generic config
        ├── application-dev.properties          # PostgreSQL (dev)
        └── application-test.properties         # H2 in-memory (test)
```

---

## 5. Prerequisites

- **JDK 25** (the `java.version` declared in `pom.xml`).
- **Maven** is not required — the project ships with the Maven Wrapper (`mvnw`, `mvnw.cmd`).
- For the `dev` profile only: **PostgreSQL 12+** with a database named `springboot_course` reachable on `localhost:5432`.

---

## 6. Configuration & Profiles

The active profile is selected in [application.properties](src/main/resources/application.properties):

```properties
spring.application.name=coursejava
spring.profiles.active=dev
spring.jpa.open-in-view=true
```

### `dev` profile — PostgreSQL

File: [application-dev.properties](src/main/resources/application-dev.properties)

- `spring.datasource.url=jdbc:postgresql://localhost:5432/springboot_course`
- DDL auto-generation: `update`
- Hibernate SQL logging enabled.

> **Heads-up:** the credentials checked in for this profile are local development defaults. For any non-local environment, override them through environment variables, a Spring config server, or your secret manager — never commit real credentials.

### `test` profile — H2 in-memory

File: [application-test.properties](src/main/resources/application-test.properties)

- In-memory database `jdbc:h2:mem:testdb`
- H2 console exposed at `/h2-console`
- Used together with [TestConfig.java](src/main/java/com/coursejava/coursejava/config/TestConfig.java) to populate seed data on startup.

To switch profile, change `spring.profiles.active` or pass `--spring.profiles.active=<profile>` at runtime.

---

## 7. Running the Application

From the project root:

### Windows (PowerShell)

```powershell
# Run with the active profile from application.properties (dev by default)
.\mvnw.cmd spring-boot:run

# Run with the in-memory H2 profile (no PostgreSQL required)
.\mvnw.cmd spring-boot:run "-Dspring-boot.run.profiles=test"
```

### Linux / macOS

```bash
./mvnw spring-boot:run
./mvnw spring-boot:run -Dspring-boot.run.profiles=test
```

### Build a runnable jar

```bash
./mvnw clean package
java -jar target/coursejava-0.0.1-SNAPSHOT.jar --spring.profiles.active=test
```

The application listens on `http://localhost:8080`.

---

## 8. REST API

Base URL: `http://localhost:8080`

### Users — `/users`  (full CRUD)

| Method | Path           | Description           | Success      |
|--------|----------------|-----------------------|--------------|
| GET    | `/users`       | List all users        | `200 OK`     |
| GET    | `/users/{id}`  | Find user by id       | `200 OK`     |
| POST   | `/users`       | Create a new user     | `201 Created` (with `Location` header) |
| PUT    | `/users/{id}`  | Update an existing user | `200 OK`   |
| DELETE | `/users/{id}`  | Delete a user         | `204 No Content` |

### Products — `/products`  (read-only)

| Method | Path              | Description           |
|--------|-------------------|-----------------------|
| GET    | `/products`       | List all products     |
| GET    | `/products/{id}`  | Find product by id    |

### Orders — `/orders`  (read-only)

| Method | Path            | Description         |
|--------|-----------------|---------------------|
| GET    | `/orders`       | List all orders     |
| GET    | `/orders/{id}`  | Find order by id    |

### Categories — `/categories`  (read-only)

| Method | Path                | Description           |
|--------|---------------------|-----------------------|
| GET    | `/categories`       | List all categories   |
| GET    | `/categories/{id}`  | Find category by id   |

### Example request — create user

```http
POST /users
Content-Type: application/json

{
  "name": "Maria Brown",
  "email": "maria@gmail.com",
  "phone": "988888888",
  "password": "123456"
}
```

Response:

```http
HTTP/1.1 201 Created
Location: http://localhost:8080/users/3
Content-Type: application/json

{
  "id": 3,
  "name": "Maria Brown",
  "email": "maria@gmail.com",
  "phone": "988888888",
  "password": "123456"
}
```

---

## 9. Error Handling

Domain exceptions are mapped to a consistent JSON payload by [`ResourceExceptionHandler`](src/main/java/com/coursejava/coursejava/resource/exceptions/ResourceExceptionHandler.java).

### `StandardError` payload

```json
{
  "timestamp": "2026-05-06T13:45:12Z",
  "status": 404,
  "error": "Resource not found",
  "message": "Resource not found. Id 999",
  "path": "/users/999"
}
```

### Exception → HTTP status mapping

| Exception                    | HTTP status         | Trigger                                                  |
|------------------------------|---------------------|----------------------------------------------------------|
| `ResourceNotFoundException`  | `404 Not Found`     | `findById` / `update` / `delete` for an unknown id       |
| `DataBaseException`          | `400 Bad Request`   | Database integrity violations (e.g. FK constraint)       |

---

## 10. Database Seeding

When the application starts under the **`test`** profile, [TestConfig](src/main/java/com/coursejava/coursejava/config/TestConfig.java) populates the H2 database with example categories, products, users, orders, order items and a payment. This is convenient for local exploration and for issuing requests against the read-only endpoints without manual data entry.

The **`dev`** profile does not run `TestConfig` — Hibernate runs with `ddl-auto=update`, but no seed data is inserted automatically.

---

## 11. Tests

Run the test suite with:

```bash
./mvnw test
```

Test scope dependencies (`spring-boot-starter-data-jpa-test`, `spring-boot-starter-webmvc-test`) are already declared in [pom.xml](pom.xml). The bundled smoke test lives at [CoursejavaApplicationTests.java](src/test/java/com/coursejava/coursejava/CoursejavaApplicationTests.java).

---

## 12. Roadmap / Possible Improvements

The project intentionally stays close to the layered Spring Boot tutorial style. Natural next steps to harden it for production use include:

- **Validation** — apply `@Valid` and Bean Validation annotations on request payloads.
- **DTOs** — decouple the HTTP contract from JPA entities (input/output DTOs at controller boundaries).
- **Security** — replace plain-text password storage with a hashing scheme (BCrypt) and add authentication/authorization (e.g. Spring Security + JWT — the `jwt.*` properties already hint at this).
- **Configuration secrets** — move database credentials out of `application-dev.properties` into environment variables or a vault.
- **Pagination** — return `Page<T>` from list endpoints instead of unbounded `List<T>`.
- **Observability** — structured logging, metrics (Micrometer), and request tracing.
- **OpenAPI** — publish a generated API spec (e.g. springdoc-openapi).
- **Test coverage** — add unit tests for services and slice tests (`@WebMvcTest`, `@DataJpaTest`) for the REST and persistence layers.
