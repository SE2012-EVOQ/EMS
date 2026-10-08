# EVOQ EMS Backend

Java 21 and Spring Boot provide the REST API, session authentication, validation and MySQL persistence. Use the [root README](../README.md#local-run-with-existing-data) to configure and run it.

## Source structure

All Java packages below are under `src/main/java/com/evoq/ems/`.

| Package | Responsibility |
| --- | --- |
| `employee` | Employees, departments, teams and lifecycle |
| `leave` | Leave types, balances, requests and decisions |
| `attendance` | Schedules, check-in/out, reconciliation and reporting |
| `asset` | Inventory, assignments, returns and history |
| `auth` | Login, password changes, first setup and account/session validation |
| `config` | Security and runtime configuration |
| `common`, `controller` | Shared responses/errors and health endpoint |

Each module separates HTTP controllers (`web/` or `controller/`), business services, repositories, and domain entities. Services enforce business rules and transaction boundaries; repositories manage MySQL persistence. [Module workflows](../docs/modules.md) document role permissions and business rules.

`src/main/resources/application.properties` holds local defaults. Spring Boot reads credentials from exported environment variables; it does not load `.env` automatically. Hibernate validates the existing schema rather than creating tables. The `dev` profile explicitly opts into demo seeding.

From this directory, `./mvnw package` builds the application into `target/`. The default health endpoint is [GET /api/health](http://localhost:8080/api/health).
